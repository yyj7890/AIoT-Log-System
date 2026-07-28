import base64, os, struct, subprocess, tempfile
from pathlib import Path
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field

app = FastAPI(docs_url=None, redoc_url=None)

class Request(BaseModel):
    text: str = Field(min_length=1, max_length=300)
    sampleRate: int = 16000
    channels: int = 1
    frameDurationMs: int = 60

def ogg_packets(data: bytes):
    packets, pending, p = [], bytearray(), 0
    while p < len(data):
        if data[p:p+4] != b'OggS' or p + 27 > len(data): raise ValueError('invalid_ogg')
        count = data[p+26]; sizes = data[p+27:p+27+count]; p += 27 + count
        for size in sizes:
            pending.extend(data[p:p+size]); p += size
            if size < 255: packets.append(bytes(pending)); pending.clear()
    return packets

@app.post('/v1/announcements/opus')
def synthesize(req: Request):
    if req.sampleRate != 16000 or req.channels != 1 or req.frameDurationMs != 60:
        raise HTTPException(400, 'unsupported_audio_format')
    model = Path(os.environ.get('PIPER_MODEL', '/models/voice.onnx'))
    if not model.is_file(): raise HTTPException(503, 'voice_model_unavailable')
    with tempfile.TemporaryDirectory(dir='/tmp') as directory:
        wav, ogg = Path(directory)/'out.wav', Path(directory)/'out.ogg'
        try:
            subprocess.run(['piper','--model',str(model),'--output_file',str(wav)], input=req.text.encode(), check=True, timeout=30, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
            subprocess.run(['ffmpeg','-v','error','-i',str(wav),'-ar','16000','-ac','1','-c:a','libopus','-application','voip','-frame_duration','60','-f','ogg',str(ogg)], check=True, timeout=30)
            packets = [p for p in ogg_packets(ogg.read_bytes()) if not p.startswith(b'OpusHead') and not p.startswith(b'OpusTags')]
        except (subprocess.SubprocessError, ValueError): raise HTTPException(502, 'tts_generation_failed')
    if not packets or len(packets) > 500: raise HTTPException(502, 'invalid_opus_output')
    return {'codec':'opus','sampleRate':16000,'channels':1,'frameDurationMs':60,'frames':[{'index':i,'payloadBase64':base64.b64encode(packet).decode()} for i, packet in enumerate(packets)]}
