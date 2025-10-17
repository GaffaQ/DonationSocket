import socket
import json

# ini host ip servernya
HOST = "127.0.0.1"
# trus ini port tujuan
PORT = 19132  

data = {
    "username": "Steve",
    "amount": 50000,
    "package": "VIP Rank",
    "token": "SECRET123"
}

with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
    s.connect((HOST, PORT))
    s.sendall(json.dumps(data).encode('utf-8'))
    print("Data terkirim ke server!")
