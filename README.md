# 🤖 Android Robotic Project

## 📱 Project Overview
This project is an **Android application** designed to control a **mobile robot (mBot Ranger)** via **Bluetooth communication**.  
The robot is capable of **autonomously following a specific object** detected through the **Android device's camera**.

The main goal is to develop an intelligent robotic system where the **Android phone** acts as the robot’s “brain” — handling image processing, object detection, and sending movement commands to the robot’s actuators.

---

## 🎯 Project Objectives
- Develop an Android app in **Java (Android Studio)**.
- Use the phone’s **camera** to detect and track a **colored object** (e.g., a red ball).
- Establish a **Bluetooth connection** between the Android device and the **mBot Ranger**.
- Send control commands (forward, backward, turn left/right, stop) to the robot based on the object’s position.
- Mount the Android device securely on the robot to act as its vision sensor.

---

## 🧩 System Architecture

### Components:
- **Android Device**  
  - Runs the app  
  - Processes camera input using computer vision  
  - Sends commands via Bluetooth  

- **mBot Ranger Robot (Makeblock)**  
  - Receives movement commands  
  - Executes actions using its motors  

### Data Flow:
Camera → Object Detection → Position Calculation → Bluetooth Command → Robot Motion


## ⚙️ Technical Details

### Technologies & Tools
- **Programming Language:** Java  
- **IDE:** Android Studio  
- **Communication:** Bluetooth Serial Port Profile (SPP)  
- **Robot Platform:** mBot Ranger (Makeblock)  
- **Image Processing:** Android Camera2 API or OpenCV (optional for color tracking)

---

## 🧠 Core Features
- Real-time **object tracking** using the phone’s camera.
- **Autonomous movement** following the detected object.
- Manual control (optional) to test robot movement via Bluetooth.
- Visual feedback (bounding box or target marker) in the camera preview.

---

## 🔗 Bluetooth Communication
The Android app communicates with the mBot Ranger using prepared Java classes for serial communication.

**Example Command Flow:**
| Command | Description |
|----------|--------------|
| `F` | Move Forward |
| `B` | Move Backward |
| `L` | Turn Left |
| `R` | Turn Right |
| `S` | Stop |

> The robot’s firmware must be configured to interpret these commands correctly.
