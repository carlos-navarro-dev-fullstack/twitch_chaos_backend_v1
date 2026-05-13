# Twitch Chat Decision Game

Backend desarrollado con Spring Boot y WebSockets inspirado en dinámicas tipo Twitch Chat Plays, donde los jugadores votan decisiones en tiempo real y el streamer recibe el resultado final de la ronda.

## Características

- Creación de salas en tiempo real
- Sistema de votaciones
- Gestión de jugadores
- Resolución automática de rondas
- Comunicación en tiempo real usando WebSockets
- API documentada con Swagger/OpenAPI
- Dockerizado para despliegue sencillo

---

## Tecnologías

- Java 17
- Spring Boot
- Spring WebSocket
- STOMP
- Maven
- Docker
- Swagger / OpenAPI
- JUnit 5
- Mockito

---

## Ejecutar localmente

### 1. Clonar repositorio

```bash
git clone <repo-url>
cd <project-folder>
```

### 2. Ejecutar con Maven

```bash
./mvnw spring-boot:run
```

En Windows:

```bash
mvnw.cmd spring-boot:run
```

---

## Ejecutar con Docker

### Construir imagen

```bash
docker build -t twitch-backend .
```

### Ejecutar contenedor

```bash
docker run --name twitch_backend -p 8080:8080 twitch-backend
```

---

## Swagger

Disponible en:

```text
http://localhost:8080/swagger-ui/index.html
```

---

## WebSocket Endpoint

```text
/ws
```

---

## Testing

Ejecutar tests:

```bash
./mvnw test
```

---

## Estado del proyecto

Proyecto en desarrollo y evolución continua.  
Actualmente enfocado en la lógica backend, WebSockets y arquitectura de juego en tiempo real.

---

## Autor

Carlos Enmanuel Navarro Caraza
