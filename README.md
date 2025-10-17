# DonationSocket

DonationSocket is a simple Minecraft plugin that receives donation notifications via a TCP socket and performs several actions on the server, such as broadcasting a message, running commands, saving donation logs, and (optionally) playing a sound chosen at random from the configuration.

Main features:
- Receive a JSON payload over a TCP socket.
- Validate incoming connections by IP whitelist and token.
- Broadcast a message to the server and run configured commands.
- Save donation logs to `donations.yml`.
- (Optional) Play one random sound from the `broadcast.sounds` list in `config.yml`.

## Build

This project uses Maven. To build the plugin JAR, run this at the project root:

```powershell
mvn clean package
```

The resulting JAR will be placed in `target/` and can be copied to your Minecraft server's `plugins/` folder.

## Configuration

Example `config.yml` (located at `src/main/resources/config.yml` and copied to the plugin folder on first run):

```yaml
server:
  port: 19132
  token: "SECRET123"

# List of IPs allowed to send socket messages
whitelist:
  - "127.0.0.1"
  - "192.168.1.10"

broadcast:
  message: "&6{username} &eberdonasi sebesar &aRp{amount}&e dan mendapatkan paket &b{package}&e! 🎉"
  # Optional: list of sounds to play when broadcasting a donation. Names should match Bukkit Sound enum values
  # Examples: "ENTITY_PLAYER_LEVELUP", "ENTITY_EXPERIENCE_ORB_PICKUP", "BLOCK_ANVIL_USE"
  # You can also write them in lower-case or with spaces/dashes; they will be normalized by the plugin.
  sounds:
    - "ENTITY_PLAYER_LEVELUP"
    - "ENTITY_EXPERIENCE_ORB_PICKUP"

# Commands to execute when a donation is processed
commands:
  - "lp user {username} parent add {package}"
  - "say Thank you {username} for the donation!"
```

- `server.port`: the TCP port the plugin listens on.
- `server.token`: a simple token to verify the sender.
- `whitelist`: list of IPs that are allowed to send payloads.
- `broadcast.sounds`: (optional) list of sound names. The plugin will pick one at random when broadcasting.

Note: Sound names must match the `Sound` enum for your server version. The plugin normalizes names (lowercase, spaces/dashes allowed), but they must match an enum value after normalization.

## Expected JSON payload

The plugin expects a single-line JSON payload containing at least `username` and `token`. Fields example:
- `username` (string)
- `amount` (number)
- `package` (string)
- `token` (string)

An example payload is sent from the Python script below.

## Example sender (send_socket.py)

Use this Python script to send a test payload to the plugin (ensure host/port and token match):

```python
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
    print("Data sent to server!")
```

if u think this plugin helpfull, pls give it a star. and if you use this plugin, dont forget to credit
---