# MediaPipe rules
-keep class com.google.mediapipe.** { *; }
-keep class com.google.protobuf.** { *; }
-keepclassmembers class * { native <methods>; }
-dontwarn com.google.mediapipe.**
-dontwarn com.google.protobuf.**

# App components used via system intents, services, tiles, and reflection
-keep class com.example.gesturecontrol.GestureService { *; }
-keep class com.example.gesturecontrol.GestureAccessibilityService { *; }
-keep class com.example.gesturecontrol.GestureTileService { *; }
-keep class com.example.gesturecontrol.StartSessionActivity { *; }
-keep class com.example.gesturecontrol.HandTracker { *; }
-keep class com.example.gesturecontrol.GestureClassifier { *; }
-keep class com.example.gesturecontrol.GestureBus { *; }
-keep class com.example.gesturecontrol.Gesture { *; }
-keep class com.example.gesturecontrol.CallState { *; }
-keep class com.example.gesturecontrol.HoldProgress { *; }
