/**
 * Expo config plugin — forces Gradle 7.5.1 + AGP 7.3.1 + Java 11 compile options.
 * Required so the Android build works with JDK 11.
 */
const {
  withProjectBuildGradle,
  withAppBuildGradle,
  withDangerousMod,
} = require("@expo/config-plugins");
const fs = require("fs");
const path = require("path");

// Force Gradle wrapper to 7.5.1 (Gradle 8+ requires Java 17)
function withGradle751(config) {
  return withDangerousMod(config, [
    "android",
    async (config) => {
      const wrapperPath = path.join(
        config.modRequest.platformProjectRoot,
        "gradle/wrapper/gradle-wrapper.properties"
      );
      if (fs.existsSync(wrapperPath)) {
        let content = fs.readFileSync(wrapperPath, "utf8");
        content = content.replace(
          /distributionUrl=.+/,
          "distributionUrl=https\\://services.gradle.org/distributions/gradle-7.5.1-all.zip"
        );
        fs.writeFileSync(wrapperPath, content);
      }
      return config;
    },
  ]);
}

// Force Android Gradle Plugin to 7.3.1 (AGP 8+ requires Java 17)
function withAgp731(config) {
  return withProjectBuildGradle(config, (config) => {
    config.modResults.contents = config.modResults.contents.replace(
      /com\.android\.tools\.build:gradle:[^\s"']+/g,
      "com.android.tools.build:gradle:7.3.1"
    );
    return config;
  });
}

// Set Java 11 source/target compatibility in app/build.gradle
function withJava11CompileOptions(config) {
  return withAppBuildGradle(config, (config) => {
    const content = config.modResults.contents;
    if (!content.includes("JavaVersion.VERSION_11")) {
      config.modResults.contents = content.replace(
        /compileOptions\s*\{[^}]*\}/s,
        `compileOptions {
        sourceCompatibility JavaVersion.VERSION_11
        targetCompatibility JavaVersion.VERSION_11
    }`
      );
    }
    return config;
  });
}

module.exports = function withJava11(config) {
  config = withGradle751(config);
  config = withAgp731(config);
  config = withJava11CompileOptions(config);
  return config;
};
