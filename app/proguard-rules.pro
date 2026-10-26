# 仅保留与导出/反射相关的最小规则；当前 release 未开启混淆。
-keepattributes *Annotation*
-keepclassmembers class **$$serializer { *; }
