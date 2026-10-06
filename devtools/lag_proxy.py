"""A TCP relay that holds every chunk a fixed delay in each direction, order kept: a real player's ping
between the fixture's client and server on one machine (D-0036).

usage: lag_proxy.py <listen port> <server port> <milliseconds each way>

The driver connects to the fixture's port (25586), so move the server: set `server-port` in
`run/network-server/server.properties` to another port, give that as <server port>, and set it
back afterwards.
"""
import asyncio
import sys
import time

LISTEN, TARGET, DELAY = int(sys.argv[1]), int(sys.argv[2]), float(sys.argv[3]) / 1000

async def pump(reader: asyncio.StreamReader, writer: asyncio.StreamWriter) -> None:
    queue: asyncio.Queue[tuple[float, bytes | None]] = asyncio.Queue()

    async def send() -> None:
        while True:
            due, data = await queue.get()
            if data is None:
                break
            wait = due - time.monotonic()
            if wait > 0:
                await asyncio.sleep(wait)
            writer.write(data)
            await writer.drain()
        writer.close()

    sender = asyncio.create_task(send())
    try:
        while data := await reader.read(65536):
            queue.put_nowait((time.monotonic() + DELAY, data))
    finally:
        queue.put_nowait((0.0, None))
        await sender

async def handle(client_r: asyncio.StreamReader, client_w: asyncio.StreamWriter) -> None:
    server_r, server_w = await asyncio.open_connection('127.0.0.1', TARGET)
    print('connection', flush=True)
    await asyncio.gather(pump(client_r, server_w), pump(server_r, client_w), return_exceptions=True)

async def main() -> None:
    server = await asyncio.start_server(handle, '127.0.0.1', LISTEN)
    print(f'proxy {LISTEN} -> {TARGET}, {DELAY * 1000:.0f} ms each way', flush=True)
    async with server:
        await server.serve_forever()

if __name__ == '__main__':
    asyncio.run(main())
