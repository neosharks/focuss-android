# Components referenced from the manifest (services, receivers, activities).
-keep class com.focuss.service.** { *; }
-keep class com.focuss.block.** { *; }
-keep class com.focuss.ui.MainActivity { *; }
-keep class com.focuss.FocussApp { *; }

# The accessibility service component name is matched as a string against the
# system's enabled-services setting, so its name must not change.
-keepnames class com.focuss.service.BlockingAccessibilityService

# Data classes persisted/parsed via org.json — keep field-bearing models.
-keep class com.focuss.data.** { *; }
