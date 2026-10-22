package dev.plume

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.media.ExifInterface
import java.io.BufferedInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket

/**
 * A one-page web server for adding wallpapers from a phone. It only runs while the upload panel is open,
 * and only answers on /<code> (the random code is in the QR code), so others on the Wi-Fi can't post.
 * The page sends the photo as the raw request body, so there's no multipart parsing.
 * [onPhoto] runs on the server thread with the uploaded file and returns the new wallpaper's name, or null.
 */
internal class UploadServer(val code: String, private val temp: File, private val onPhoto: (File) -> String?) {
    private val socket = try {
        ServerSocket(8080)
    } catch (e: IOException) {
        ServerSocket(0) // 8080 taken: any free port (it's in the QR code either way)
    }
    val port get() = socket.localPort

    init {
        Thread(::serve, "wallpaper-upload").start()
    }

    fun stop() = socket.close()

    private fun serve() {
        while (!socket.isClosed) {
            val s = try {
                socket.accept()
            } catch (e: IOException) {
                return // closed
            }
            try {
                s.use { handle(it) }
            } catch (e: Exception) {
                android.util.Log.w("slawastr", "upload request failed ($e)")
            }
        }
    }

    private fun handle(s: Socket) {
        s.soTimeout = 30_000
        val input = BufferedInputStream(s.getInputStream())
        val out = s.getOutputStream()
        val request = readLine(input) ?: return
        val headers = generateSequence { readLine(input)?.takeIf { it.isNotEmpty() } }
            .associate { it.substringBefore(':').trim().lowercase() to it.substringAfter(':').trim() }
        val method = request.substringBefore(' ')
        val path = request.substringAfter(' ').substringBefore(' ')
        when {
            path != "/$code" -> reply(out, 404, "text/plain", "Not found. Scan the code on the TV again.")
            method == "GET" -> reply(out, 200, "text/html; charset=utf-8", PAGE)
            method == "POST" -> {
                val length = headers["content-length"]?.toLongOrNull() ?: -1
                if (length !in 1..MAX_BYTES) return reply(out, 413, "text/plain", "That file is too big (25 MB at most).")
                temp.outputStream().use { file ->
                    val buf = ByteArray(64 * 1024)
                    var left = length
                    while (left > 0) {
                        val n = input.read(buf, 0, minOf(buf.size.toLong(), left).toInt())
                        if (n < 0) throw IOException("upload cut short")
                        file.write(buf, 0, n)
                        left -= n
                    }
                }
                val name = try {
                    onPhoto(temp)
                } finally {
                    temp.delete()
                }
                if (name != null) reply(out, 200, "text/plain", "Added \"$name\" to your TV.")
                else reply(out, 415, "text/plain", "That doesn't look like a photo the TV can open.")
            }
            else -> reply(out, 405, "text/plain", "Not allowed")
        }
    }

    /** One header line, without the line break; null at the end of the stream. Capped so junk can't fill memory. */
    private fun readLine(input: InputStream): String? {
        val sb = StringBuilder()
        while (sb.length < 8192) {
            val c = input.read()
            if (c < 0) return if (sb.isEmpty()) null else sb.toString()
            if (c == '\n'.code) return sb.toString().trimEnd('\r')
            sb.append(c.toChar())
        }
        throw IOException("header line too long")
    }

    private fun reply(out: OutputStream, status: Int, type: String, body: String) {
        val bytes = body.toByteArray()
        out.write("HTTP/1.1 $status ${if (status == 200) "OK" else "Error"}\r\nContent-Type: $type\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n".toByteArray())
        out.write(bytes)
        out.flush()
    }

    private companion object {
        const val MAX_BYTES = 25L * 1024 * 1024

        val PAGE = """
            <!doctype html>
            <html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
            <title>slawastr\'s launcher · Загрузка обоев</title>
            <style>
              body{font:17px system-ui,-apple-system,sans-serif;margin:0;padding:32px 20px;background:#f7f8fa;color:#111214;text-align:center}
              @media (prefers-color-scheme:dark){body{background:#09090b;color:#f5f5f7}}
              h1{font-size:24px;margin:0 0 8px} p{opacity:.7;margin:0 0 24px}
              label,button{display:block;width:100%;box-sizing:border-box;padding:16px;border-radius:12px;font:inherit;text-align:center;margin-bottom:12px}
              label{border:1.5px dashed currentColor;cursor:pointer} button{border:0;background:#111214;color:#fff;font-weight:600}
              @media (prefers-color-scheme:dark){button{background:#f5f5f7;color:#111214}}
              button:disabled{opacity:.35} input{display:none} #s{min-height:1.5em;margin-top:14px;font-size:15px;line-height:1.4}
              .progress{margin-top:10px;font-weight:bold}
            </style></head><body>
            <h1>Загрузка обоев на ТВ</h1>
            <p>Выберите одно или несколько фото. Они будут отправлены на ваш телевизор.</p>
            <label><input id="f" type="file" accept="image/*" multiple><span id="n">Выбрать фото (можно несколько)</span></label>
            <button id="b" disabled>Отправить на телевизор</button>
            <div id="s"></div>
            <script>
              const f=document.getElementById('f'),b=document.getElementById('b'),s=document.getElementById('s'),n=document.getElementById('n');
              f.onchange=()=>{
                const count = f.files.length;
                b.disabled = !count;
                n.textContent = count ? (count === 1 ? f.files[0].name : 'Выбрано фото: ' + count) : 'Выбрать фото (можно несколько)';
                s.textContent = '';
              };
              b.onclick=async()=>{
                b.disabled = true;
                const total = f.files.length;
                let success = 0;
                for(let i = 0; i < total; i++){
                  const file = f.files[i];
                  s.innerHTML = 'Отправка (' + (i + 1) + ' из ' + total + '): ' + file.name + '…';
                  try {
                    const r = await fetch(location.pathname, { method: 'POST', body: file });
                    if(r.ok) success++;
                  } catch(e) {
                    s.innerHTML = '<span style="color:red">Ошибка отправки ' + file.name + '</span>';
                    b.disabled = false;
                    return;
                  }
                }
                s.innerHTML = '<span style="color:#2ecc71">Успешно загружено: ' + success + ' фото!</span><br>Они уже на телевизоре.';
                b.disabled = false;
              };
            </script></body></html>
        """.trimIndent()
    }
}

/**
 * Turns a photo into a wallpaper: upright (phones often store photos sideways with an orientation tag),
 * cropped to 16:9 around the centre, 1920×1080, mutable. [open] is called once per pass over the image.
 * Returns null if it isn't an image Android can decode.
 */
internal fun fitWallpaper(crop: String = "center", alignX: Float = 0.5f, alignY: Float = 0.5f, open: () -> InputStream): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    open().use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    val rotation = try {
        when (open().use { ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) }) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    } catch (e: IOException) {
        0
    }
    val (w, h) = if (rotation % 180 == 0) bounds.outWidth to bounds.outHeight else bounds.outHeight to bounds.outWidth
    // Decode at the smallest size whose 16:9 crop still covers the screen: a 12 MP photo in full is ~48 MB.
    val cropW = minOf(w, h * 16 / 9)
    var sample = 1
    while (cropW / (sample * 2) >= WALL_W) sample *= 2
    val decoded = open().use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) } ?: return null
    val upright = if (rotation == 0) decoded else
        Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, Matrix().apply { postRotate(rotation.toFloat()) }, true)
            .also { decoded.recycle() }
    val cw = minOf(upright.width, upright.height * 16 / 9)
    val ch = cw * 9 / 16
    val (ax, ay) = when (crop) {
        "top" -> 0.5f to 0.0f
        "bottom" -> 0.5f to 1.0f
        "manual" -> alignX.coerceIn(0f, 1f) to alignY.coerceIn(0f, 1f)
        else -> 0.5f to 0.5f
    }
    val left = ((upright.width - cw) * ax).toInt()
    val top = ((upright.height - ch) * ay).toInt()
    val out = Bitmap.createBitmap(WALL_W, WALL_H, Bitmap.Config.ARGB_8888)
    Canvas(out).drawBitmap(upright, Rect(left, top, left + cw, top + ch), Rect(0, 0, WALL_W, WALL_H), Paint(Paint.FILTER_BITMAP_FLAG))
    upright.recycle()
    return out
}

/** Saves a phone photo as a wallpaper file (~300-500 KB JPEG). Returns the file, or null if it isn't an image. */
internal fun importWallpaper(src: File, dir: File): File? {
    val out = fitWallpaper { src.inputStream() } ?: return null
    dir.mkdirs()
    val file = generateSequence(1) { it + 1 }.map { File(dir, "Photo $it.jpg") }.first { !it.exists() }
    file.outputStream().use { out.compress(Bitmap.CompressFormat.JPEG, 85, it) }
    out.recycle()
    return file
}

private const val WALL_W = 1920
private const val WALL_H = 1080
