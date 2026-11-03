#!/usr/bin/env python3
"""MAVLink UDP <-> TCP bridge for running the GCS app on a USB-connected phone.

`adb reverse` only forwards TCP, and PX4 SITL only speaks MAVLink over UDP.
This bridge receives PX4's onboard link (UDP 14540) and relays it to one TCP
client, so the app can connect with `tcpout://127.0.0.1:5760` through:

    PX4 SITL --UDP:14540--> bridge --TCP:5760--> adb reverse --> phone app

Usage:
    adb reverse tcp:5760 tcp:5760
    python3 tools/mavlink_udp_tcp_bridge.py

MAVLink frames carry their own start byte and length, so relaying raw bytes
over a TCP stream is safe; MAVSDK's parser re-frames them.
"""
import argparse
import socket
import threading


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--udp-port", type=int, default=14540, help="port PX4 sends to")
    parser.add_argument("--tcp-port", type=int, default=5760, help="port the app connects to")
    args = parser.parse_args()

    udp = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    udp.bind(("127.0.0.1", args.udp_port))

    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    server.bind(("127.0.0.1", args.tcp_port))
    server.listen(1)

    px4_addr = None
    client = None
    lock = threading.Lock()

    def udp_to_tcp() -> None:
        nonlocal px4_addr, client
        while True:
            data, addr = udp.recvfrom(65535)
            if px4_addr != addr:
                print(f"PX4 at {addr[0]}:{addr[1]}", flush=True)
                px4_addr = addr
            with lock:
                c = client
            if c is None:
                continue
            try:
                c.sendall(data)
            except OSError:
                pass  # client gone; tcp thread will accept a new one

    threading.Thread(target=udp_to_tcp, daemon=True).start()
    print(f"UDP :{args.udp_port} <-> TCP :{args.tcp_port}, waiting for app...", flush=True)

    while True:
        conn, addr = server.accept()
        conn.setsockopt(socket.IPPROTO_TCP, socket.TCP_NODELAY, 1)
        print(f"app connected from {addr[0]}:{addr[1]}", flush=True)
        with lock:
            client = conn
        try:
            while True:
                data = conn.recv(65535)
                if not data:
                    break
                if px4_addr is not None:
                    udp.sendto(data, px4_addr)
        except OSError:
            pass
        finally:
            with lock:
                client = None
            conn.close()
            print("app disconnected", flush=True)


if __name__ == "__main__":
    main()
