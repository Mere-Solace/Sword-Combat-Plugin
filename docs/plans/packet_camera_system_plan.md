# Packet-Driven Camera System (Paper / NMS)

## Overview

This document defines a full implementation plan for a packet-driven
camera system using `ClientboundSetCameraPacket`.

Goal: - Detach camera from player without spectator mode - Maintain
player input - Achieve smooth, interpolated camera motion

------------------------------------------------------------------------

## Core Concept

The Minecraft client allows its camera to be reassigned to another
entity via packet:

`ClientboundSetCameraPacket`

This bypasses Bukkit's limitations.

------------------------------------------------------------------------

## Architecture

### Components

1.  Camera Entity (server or client-side)
2.  Camera Controller
3.  Packet Dispatcher (NMS / ProtocolLib)
4.  Motion System (position + rotation)
5.  State Manager (attach/detach lifecycle)

------------------------------------------------------------------------

## Step 1: Camera Entity

### Option A: Server-side ArmorStand

-   Invisible
-   No gravity
-   Marker optional

### Option B: Client-side entity (advanced)

-   Spawn via packets only
-   Not tracked by server

------------------------------------------------------------------------

## Step 2: Send Camera Packet

### NMS (example concept)

-   Obtain ServerPlayer
-   Send packet:

`new ClientboundSetCameraPacket(entity)`

### ProtocolLib alternative

-   Use PacketType.Play.Server.CAMERA

------------------------------------------------------------------------

## Step 3: Attach Flow

1.  Ensure player is NOT in spectator
2.  Spawn camera entity
3.  Send camera packet
4.  Store reference

------------------------------------------------------------------------

## Step 4: Movement System

### Preferred: velocity-based

-   Move entity smoothly
-   Avoid teleport spam when possible

### Alternative: teleport per tick

-   Simpler
-   Slight jitter risk

------------------------------------------------------------------------

## Step 5: Rotation Control

Camera rotation comes from:

-   entity rotation (partially)
-   OR explicit player rotation packets

Recommended:

-   send rotation updates using teleport/look packets
-   OR adjust entity head rotation if supported

------------------------------------------------------------------------

## Step 6: Input Handling

Player remains in survival.

You can: - read movement keys - override behavior - build custom
controls

------------------------------------------------------------------------

## Step 7: Detach Flow

1.  Send camera packet → player
2.  Remove camera entity
3.  restore state

------------------------------------------------------------------------

## Step 8: Synchronization

Ensure: - entity exists client-side before camera attach - small delay
(1 tick) recommended

------------------------------------------------------------------------

## Step 9: Interpolation

Client interpolates entity movement automatically if: - movement packets
are consistent - not excessive teleporting

------------------------------------------------------------------------

## Step 10: Edge Cases

-   entity removed → camera breaks
-   player world change → must reattach
-   death/respawn → reset camera

------------------------------------------------------------------------

## Performance Notes

-   avoid per-tick heavy packet spam
-   reuse entities when possible
-   batch updates if needed

------------------------------------------------------------------------

## Advantages

-   smooth camera
-   full control
-   no spectator lock
-   works during combat

------------------------------------------------------------------------

## Disadvantages

-   version dependent (NMS)
-   requires abstraction layer
-   debugging is harder

------------------------------------------------------------------------

## Recommended Abstractions

Create:

-   CameraService
-   CameraSession
-   PacketAdapter

------------------------------------------------------------------------

## Future Extensions

-   spline-based camera paths
-   cinematic transitions
-   blending between cameras
-   multiple camera targets

------------------------------------------------------------------------

## Summary

Packet-driven camera systems provide full control over player
perspective by assigning the camera to a custom entity via packets. This
approach avoids spectator limitations and enables smooth, flexible
camera systems suitable for advanced gameplay and cinematic features.
