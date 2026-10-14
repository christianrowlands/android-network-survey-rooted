# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Preserve the line number information for debugging stack traces, but hide the original source file name.
# The mapping.txt file is needed to deobfuscate stack traces (it is uploaded to Crashlytics and embedded in the bundle).
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Protobuf: the MQTT library converts the messages to JSON with JsonFormat, which looks up the generated accessor
# methods by name using reflection. Keep the members of the generated classes so those lookups still work.
-keepclassmembers class * extends com.google.protobuf.GeneratedMessage { *; }
-keepclassmembers class * extends com.google.protobuf.GeneratedMessage$Builder { *; }
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageV3 { *; }
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageV3$Builder { *; }
-keepclassmembers enum * implements com.google.protobuf.ProtocolMessageEnum { *; }

# HiveMQ MQTT client: Netty and JCTools access fields by name (atomic field updaters and Unsafe offsets)
-keepclassmembernames class io.netty.** { *; }
-keepclassmembernames class org.jctools.** { *; }

# HiveMQ MQTT client and Netty: optional dependencies that are only used when present on the classpath
# (logging backends, alternate SSL providers, native transports, and the WebSocket and proxy modules)
-dontwarn io.netty.channel.epoll.**
-dontwarn io.netty.handler.codec.http.**
-dontwarn io.netty.handler.proxy.**
-dontwarn io.netty.internal.tcnative.**
-dontwarn org.apache.log4j.**
-dontwarn org.apache.logging.log4j.**
-dontwarn org.conscrypt.**
-dontwarn org.eclipse.jetty.alpn.**
-dontwarn org.eclipse.jetty.npn.**
-dontwarn org.slf4j.**
-dontwarn reactor.blockhound.**

# Firebase Analytics (play flavor) references the advertising ID client, which build.gradle intentionally excludes
-dontwarn com.google.android.gms.ads.identifier.**
