# Компоненты сборки

Исходный проект: https://github.com/emount4/pool, коммит a6286ff.

Сборка использует OpenJFX 21.0.8, PostgreSQL JDBC 42.7.13, Apache POI 5.5.1,
SLF4J 2.0.17, Log4j-to-SLF4J 2.24.3 и их транзитивные зависимости.
Maven Wrapper 3.3.4 скачивает Apache Maven 3.9.11 для сборки из исходников.

Оригинальные LICENSE/NOTICE компонентов сохранены внутри JAR-файлов.
Полные координаты и версии зависимостей приведены в pom.xml; дерево можно
получить командой `mvnw dependency:tree`.

JUnit, Mockito и Monocle используются только в тестах и не включаются в
runtime-каталог app. PostgreSQL, Java runtime и Docker устанавливаются отдельно.
