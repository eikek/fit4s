import tempfile
import webbrowser
import json
from http.server import HTTPServer, SimpleHTTPRequestHandler
import os
import argparse


# parser = argparse.ArgumentParser("test")
# parser.add_argument("pl", help="The file with the polyline encoded")
# parser.add_argument("nl", help="The file with the normal polyline")

# args = parser.parse_args()
# pl=args.pl
# nl=args.nl

dir_path = os.path.dirname(os.path.realpath(__file__))



class Handler(SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs, directory=dir_path)

    def do_GET(self):
        """Serve GET"""
        try:
            fname = ""
            type = "unknown"
            if self.path == "/track":
                fname = dir_path + "/../out/core/test/test.dest/sandbox/track"
                type = "encoded"
            elif self.path == "/track-poly":
                fname = dir_path + "/../out/core/test/test.dest/sandbox/track-poly"
                type = "encoded"
            elif self.path == "/track.coord":
                fname = dir_path + "/../out/core/test/test.dest/sandbox/track.coord"
                type = "coord"
            elif self.path == "/viewer.html":
                fname = dir_path + self.path
                type = "viewer"

            print(f"Opening {fname} (type={type})")
            with open(fname, 'rb') as file:
                self.send_response(200)
                if type == "viewer":
                    self.send_header("Content-Type", "text/html")
                    self.end_headers()
                    self.wfile.write(file.read())
                elif type == "encoded":
                    content = file.read()
                    content = content.decode("utf-8")
                    content = json.dumps(content)
                    self.send_header("Content-Type", "application/json")
                    self.end_headers()
                    self.wfile.write(content.encode("utf-8"))
                elif type == "coord":
                    content = file.read()
                    content = eval(content.decode("utf-8"))
                    content = json.dumps(content)
                    self.send_header("Content-Type", "application/json")
                    self.end_headers()
                    self.wfile.write(content.encode("utf-8"))

        except FileNotFoundError:
            self.send_error(404, "not found")

server = HTTPServer(("localhost", 3000), Handler)
webbrowser.open("http://localhost:3000/viewer.html")
server.serve_forever()
