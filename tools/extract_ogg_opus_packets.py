#!/usr/bin/env python3
"""Extract raw Opus packets from a trusted Ogg/Opus asset for local announcement tests."""

from __future__ import annotations

import argparse
import json
from pathlib import Path


def read_packets(source: Path) -> tuple[int, list[bytes]]:
    data = source.read_bytes()
    offset = 0
    partial = bytearray()
    packets: list[bytes] = []
    sample_rate = 0

    while offset < len(data):
        if data[offset:offset + 4] != b"OggS" or offset + 27 > len(data):
            raise ValueError("invalid Ogg page")
        segment_count = data[offset + 26]
        header_end = offset + 27 + segment_count
        if header_end > len(data):
            raise ValueError("truncated Ogg lacing table")
        lacing = data[offset + 27:header_end]
        body_end = header_end + sum(lacing)
        if body_end > len(data):
            raise ValueError("truncated Ogg page body")
        body_offset = header_end
        for segment_length in lacing:
            partial.extend(data[body_offset:body_offset + segment_length])
            body_offset += segment_length
            if segment_length < 255:
                packets.append(bytes(partial))
                partial.clear()
        offset = body_end

    if partial:
        raise ValueError("unterminated Ogg packet")
    if len(packets) < 3 or not packets[0].startswith(b"OpusHead") or not packets[1].startswith(b"OpusTags"):
        raise ValueError("asset is not Ogg/Opus")
    if len(packets[0]) >= 16:
        sample_rate = int.from_bytes(packets[0][12:16], "little")
    audio_packets = packets[2:]
    if sample_rate != 16000 or not audio_packets:
        raise ValueError("test asset must be 16 kHz Ogg/Opus with audio packets")
    if len(audio_packets) > 40 or any(not packet or len(packet) > 1500 for packet in audio_packets):
        raise ValueError("test asset exceeds firmware announcement limits")
    return sample_rate, audio_packets


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("source", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()

    sample_rate, packets = read_packets(args.source)
    args.output.mkdir(parents=True, exist_ok=True)
    frames = []
    for index, packet in enumerate(packets):
        name = f"{index:06d}.opus"
        (args.output / name).write_bytes(packet)
        frames.append({"index": index, "resource": name})
    (args.output / "manifest.json").write_text(json.dumps({
        "codec": "opus",
        "sampleRate": sample_rate,
        "channels": 1,
        "frameDurationMs": 60,
        "frames": frames,
    }, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
