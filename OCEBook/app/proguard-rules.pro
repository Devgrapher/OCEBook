# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/Brent/Library/Android/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
-keepclassmembers class com.devgrapher.ocebook.readium.ReadiumContext.JsInterface {
   public *;
}

-keepnames class com.koushikdutta.async.** { *; }
-keep class com.koushikdutta.async.* { *;}

-dontwarn android.support.v4.**
-keepnames class android.support.v4.** { *; }
-keep class android.support.v4.* { *; }

-dontwarn android.support.v7.**
-keepnames class android.support.v7.** { *; }
-keep class android.support.v7.* { *; }

-keepnames class org.** { *; }
-keep class org.** { *; }

-keepclassmembers class *.R$ {
public static *;
}

-keep class *.R$

-keepattributes Annotation
