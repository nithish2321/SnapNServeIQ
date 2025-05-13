from flask import Flask, request, jsonify
import os
import base64
from io import BytesIO
from PIL import Image, ImageDraw, ImageFont
import torch
from ultralytics import YOLO

# Initialize Flask app
app = Flask(__name__)

# Define folders
UPLOAD_FOLDER = r'D:\KANTH\Downloads\android_upload'
PROCESSED_FOLDER = os.path.join(UPLOAD_FOLDER, "processed")
os.makedirs(UPLOAD_FOLDER, exist_ok=True)
os.makedirs(PROCESSED_FOLDER, exist_ok=True)

# Load YOLO model
MODEL_PATH = "best.pt"
model = YOLO(MODEL_PATH)

# Helper function: Save image from Base64 string
def save_image_from_base64(base64_str, folder, filename):
    image = Image.open(BytesIO(base64.b64decode(base64_str)))
    filepath = os.path.join(folder, filename)
    image.save(filepath)
    return image, filepath

# Helper function: Annotate image with YOLO results
def annotate_image(image, results, model_names):
    draw = ImageDraw.Draw(image)
    font_path = "arial.ttf"  # Ensure this font exists
    font = ImageFont.truetype(font_path, size=30)
    detected_objects = []

    for result in results[0].boxes:
        bbox = result.xyxy[0].tolist()
        cls_id = int(result.cls[0])
        confidence = result.conf[0]
        label = f"{model_names[cls_id]} {confidence.item():.2f}"

        detected_objects.append({
            "class": model_names[cls_id],
            "confidence": round(confidence.item(), 2),
            "bbox": [int(coord) for coord in bbox]
        })

        color = tuple(int(x) for x in torch.randint(0, 256, (3,)))
        x_min, y_min, x_max, y_max = map(int, bbox)
        for thickness in range(5):
            draw.rectangle([x_min - thickness, y_min - thickness, x_max + thickness, y_max + thickness], outline=color)

        text_bbox = draw.textbbox((0, 0), label, font=font)
        text_width = text_bbox[2] - text_bbox[0]
        text_height = text_bbox[3] - text_bbox[1]
        label_x, label_y = x_min, max(0, y_min - text_height)
        draw.rectangle([label_x, label_y, label_x + text_width, label_y + text_height], fill=color)
        draw.text((label_x, label_y), label, fill="white", font=font)

    return image, detected_objects

# Helper function: Process image with YOLO
def process_image(image):
    results = model.predict(image, conf=0.5)
    return results

@app.route('/upload', methods=['POST'])
def upload_image():
    try:
        image_data = request.form.get("image")
        if not image_data:
            return jsonify({"message": "No image provided"}), 400

        # Save original image
        original_filename = f"image_{len(os.listdir(UPLOAD_FOLDER)) + 1}.jpg"
        image, original_filepath = save_image_from_base64(image_data, UPLOAD_FOLDER, original_filename)

        return jsonify({
            "message": "Image uploaded successfully",
            "original_image_path": original_filepath
        }), 200

    except Exception as e:
        return jsonify({"message": "Error uploading image", "error": str(e)}), 500

@app.route('/process', methods=['POST'])
def process_uploaded_image():
    try:
        image_filename = request.form.get("filename")
        if not image_filename:
            return jsonify({"message": "No filename provided"}), 400

        # Ensure we extract only the filename, not the full path
        image_basename = os.path.basename(image_filename)
        original_filepath = os.path.join(UPLOAD_FOLDER, image_basename)

        if not os.path.exists(original_filepath):
            return jsonify({"message": "File not found"}), 404

        # Inform that processing is starting
        processing_message = {"message": "Started processing image"}
        
        # Open image
        image = Image.open(original_filepath)

        # Process image
        results = process_image(image)
        image, detected_objects = annotate_image(image, results, model.names)

        # Save processed image
        processed_filename = f"processed_{image_basename}"
        processed_filepath = os.path.join(PROCESSED_FOLDER, processed_filename)
        image.save(processed_filepath)

        # Convert processed image to Base64
        buffered = BytesIO()
        image.save(buffered, format="JPEG")
        encoded_image = base64.b64encode(buffered.getvalue()).decode()

        return jsonify({
            "message": "Image processed successfully",
            "processed_image": encoded_image,
            "detected_objects": detected_objects
        }), 200

    except Exception as e:
        return jsonify({"message": "Error processing image", "error": str(e)}), 500
    
@app.route('/')
def hello_world():
    return '<h2>Hello, World!</h2>'

if __name__ == '__main__':
    app.run(debug=False, host='0.0.0.0', port=5000)
