# NaTalks  1-to-1 Video Calling App

NaTalks is an Android app that lets two people make a live video call to each other, a bit like a simple WhatsApp or FaceTime video call.
---

##ScreenRecording of App

https://github.com/user-attachments/assets/ca2f8436-6b45-4f17-b769-eb34c01dbb68


## What the app can do

- **Sign up and log in** with a username and password.
- **See your contacts** and a green dot next to anyone who is online right now.
- **Call someone** with one tap. Their phone rings, even when the app is in the background.
- **Accept or decline** an incoming call.
- **Video and voice call** with camera and microphone controls.
- **Get clear outcomes** when a call can't go through: the other person is busy, offline, or declined.
- **Meeting notes (in progress):** the person who started the call can turn on a "notetaker", and a **Meetings** screen lists past calls with space for a summary and transcript.

---

## The big idea in one sentence

> **The server never sees or carries your video.** It only introduces the two phones to each other. After that, the phones send video and audio **directly to each other**.

Think of the server as someone who sets up a phone call between two people: they connect you, then step away. The conversation itself doesn't pass through them.

```
                ┌──────────────────────────┐
                │      Backend server       │
                │  • login & accounts       │
                │  • who is online          │
                │  • passes "call" messages │
                └───────┬──────────┬────────┘
              small text│          │small text
               messages │          │ messages
                ┌───────▼──┐    ┌──▼───────┐
                │ Phone A  │    │ Phone B  │
                └───────┬──┘    └──┬───────┘
                        └────┬─────┘
                  video & audio go directly
                  phone-to-phone (encrypted)
```

This keeps the server cheap and light, and the call stays private because the media is encrypted end to end between the two phones.

---

## How a call works (step by step)

1. **Both phones go online.** When you open the app after logging in, it keeps a live connection to the server, which then knows you're available.
2. **Alice taps Bob's name.** Her phone turns on the camera and sends the server a short "I want to call Bob" message, which includes technical details about how her phone can be reached.
3. **The server passes it to Bob.** If Bob is online and free, his phone rings. If he's on another call or offline, the server tells Alice right away.
4. **Bob accepts.** His phone replies with its own connection details, and the server passes that back to Alice.
5. **The phones connect directly.** Both phones try different network paths until they find one that works. A free public helper service (a "STUN server") lets each phone learn its own public internet address so the other can reach it.
6. **Video starts flowing** straight between the two phones.
7. **Someone hangs up.** A "call ended" message goes through the server, both phones turn off the camera, and the app goes back to the contacts screen.

If one phone suddenly loses its connection mid-call, the server notices and tells the other phone, so it doesn't sit waiting forever.

---

## App architecture (Android)

The app is written in **Kotlin** with **Jetpack Compose**, Android's modern way to build screens. It's organised into a few clear layers:

| Layer | What it does (in plain terms) |
|---|---|
| **Screens (`ui/`)** | Everything you see: login, register, contacts, the calling screens, and meetings. |
| **Call manager (`call/`)** | The "brain" of a call. It tracks whether you're idle, ringing, in a call or finished, and decides what happens next. It also handles the ringtone, vibration and incoming-call notification. |
| **Video engine (`webrtc/`)** | Turns on the camera and microphone and manages the direct phone-to-phone connection. It uses **WebRTC**, the same technology behind Google Meet and many other video apps. |
| **Data & network (`data/`)** | Talks to the server: logging in, loading contacts, and sending and receiving call messages. It also saves your login on the phone so you stay signed in. |
| **Navigation (`navigation/`)** | Moves you between screens and jumps straight to the call screen when a call starts or comes in. |
| **Wiring (`di/`)** | One place that creates the shared parts of the app and hands them to whoever needs them. |

A **background service** keeps the app connected to the server while you're logged in, so incoming calls can ring even when the app isn't open on screen.

---

**Where data is stored:**
- **User accounts and meeting records** live in a **PostgreSQL** database (hosted on Neon).
- **Live call information** (who is calling whom right now) is kept only in the server's memory, because it's short-lived and doesn't need saving.
- **Video and audio are never stored or sent through the server.**

---

## How the app and server talk

The app connects to the server in two ways:

1. **Regular web requests**, for one-off actions: log in, register, load contacts, fetch connection helpers, view meetings.
2. **A live, always-open connection** (a WebSocket), for call messages that have to arrive instantly, like "your phone is ringing".

Every request and live connection carries the login token, and the server rejects anything without a valid one.

---
