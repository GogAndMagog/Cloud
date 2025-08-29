# Используем официальный OpenJDK образ в качестве базового
FROM openjdk:21

# Устанавливаем рабочую директорию внутри контейнера
WORKDIR /app

# Копируем скомпилированный JAR-файл внутрь контейнера
COPY build/libs/Cloud-0.0.1-SNAPSHOT.jar app.jar
COPY .env .env

# Указываем команду для запуска приложения
ENTRYPOINT ["java", "-jar", "app.jar"]