# SnapNServeIQ
"SnapNServeIQ" is a cutting-edge app enabling real-time image processing from Android to laptop. Instantly upload, process, and receive object inference back on your mobile. Utilizing ngrok for secure communication, it seamlessly bridges mobile and desktop for robust, efficient image analysis and intelligent insights.

Files used
●	Server.zip (Python-based application @ laptop)
●	client.zip (Android app @ Android mobile phone)
Procedure
Step 1: Server side (A laptop): 
●	Install Python 3.10.14 on the the laptop
●	Open a terminal and check the python version as follows: 
![image](https://github.com/user-attachments/assets/577c3a5a-130e-4273-9a1b-799e4497bcc6)
Step 2: Extract the server.zip into the C drive (on the laptop). This will store the following files in the C:\server\: 
●	server_vit.py
●	best.pt 
![image](https://github.com/user-attachments/assets/dfc4c5d9-3b90-4aa8-a71d-8d7d36f5c45f)
Step 3:  Give the command pip install flask pillow torch torchvision torchaudio ultralytics to download the libraries in the laptop as follows:
![image](https://github.com/user-attachments/assets/0901a98a-78cd-4f39-9311-cb19eadf043b)
Step 4: In server_vit.py file, give the suitable folder path to store the images uploaded by the Android App as follows:  
![image](https://github.com/user-attachments/assets/55e7e659-9423-4071-b365-6ec185eb592b)
Step 5: Run the python server_vit.py as follows:  
![image](https://github.com/user-attachments/assets/0d6d091d-9efd-47de-927f-d633b9b14f86)
Step 6: Exposing the Local Server (running on the laptop) using ngrok using the command                         
ngrok http 5000 as follows:
![image](https://github.com/user-attachments/assets/07431e88-bb43-461d-ad83-c417ea8626c4)
![image](https://github.com/user-attachments/assets/e84227be-2acb-48c1-9378-cd289bbd9c7f)
●	Copy/preserve the ngrok link for connecting the Android app to the Python server (laptop).
Step 7: Now extract the client.zip and load the Android app into an android studio. Then create the .apk file and send it to the required mobile phone (via whatsapp). 
![image](https://github.com/user-attachments/assets/2423c36c-a970-44fc-8c82-fcb4875e99df)
Step 8: Now run the Android app in the mobile phone (client side). 
Step 9: Enter the ngrok url (Step 6) to the application as follows: 
![image](https://github.com/user-attachments/assets/b3a4cbab-8377-453e-9d9a-31c0322c6f4e)
Step 10 : Select any image from the mobile phone (camera / gallery ). 
●	For accessing the camera: 
○	single tap on camera lens image (at the home page)
![image](https://github.com/user-attachments/assets/20fedc5f-efb1-4db5-b97b-43f3e91f904e)
Step 11: After uploading the image, we will inference as follows: 
![image](https://github.com/user-attachments/assets/abbae1ea-9e5c-411c-8708-1d8221d20377) ![image](https://github.com/user-attachments/assets/19f0a16f-3aa2-49fa-a42c-a6a1d5f46c37)
![image](https://github.com/user-attachments/assets/7e40b544-8264-4c71-8059-fd9c11720ed1)








