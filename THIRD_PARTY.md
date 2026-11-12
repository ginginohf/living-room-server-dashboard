# Provenance and dependencies

Original server-specific code was extracted into a new repository without deployment history or production data. This is not a claim that all algorithms are novel. External runtimes and dependencies are installed separately and retain their own licenses. Original project code is licensed under MIT; dependencies retain their respective licenses.

`android/gradlew` и `android/gradle/wrapper/gradle-wrapper.jar` — Gradle Wrapper, сторонний компонент Gradle (Apache-2.0). Дистрибутив8.9 загружается по URL из wrapper properties и проверяется по distributionSha256Sum. Личные Secrets.java, APK и ключи подписи не перенесены.
