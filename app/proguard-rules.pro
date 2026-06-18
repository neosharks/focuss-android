# Components referenced from the manifest (services, receivers, activities).
-keep class com.neosharks.focuss.service.** { *; }
-keep class com.neosharks.focuss.block.** { *; }
-keep class com.neosharks.focuss.ui.MainActivity { *; }
-keep class com.neosharks.focuss.FocussApp { *; }

# The accessibility service component name is matched as a string against the
# system's enabled-services setting, so its name must not change.
-keepnames class com.neosharks.focuss.service.BlockingAccessibilityService

# Data classes persisted/parsed via org.json — keep field-bearing models.
-keep class com.neosharks.focuss.data.** { *; }
