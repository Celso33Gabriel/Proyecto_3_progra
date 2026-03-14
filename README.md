# ProyectoRabbit - Celso Sarceño

## Arquitectura del Sistema
Este sistema simula un flujo de procesamiento de transacciones bancarias utilizando una arquitectura orientada a mensajes.

### Producer (Emisor)
* **Consumo de datos:** Se conecta a una API externa mediante un método GET para obtener un lote de transacciones.
* **Clasificación:** Organiza las transacciones según el banco de destino (BAC, BI, BANRURAL, GYT).
* **Broker (RabbitMQ):** Envía los mensajes a colas específicas en RabbitMQ, actuando como un buffer intermedio para asegurar que no se pierda ninguna operación.
* **Firma Digital:** Cada transacción es marcada con el nombre y carnet del alumno antes de ser enviada.

### Consumer (Receptor)
* **Escucha activa:** Se mantiene conectado a las colas de RabbitMQ esperando nuevos mensajes.
* **Procesamiento:** Al recibir una transacción, la deserializa y realiza un POST hacia la API final en Amazon AWS para su almacenamiento definitivo.
* **Confirmación:** Una vez almacenada en la nube, el mensaje es eliminado de la cola de RabbitMQ.

## Tecnologías Utilizadas
* **Java 11**
* **Gestor de Dependencias:** Maven
* **Broker de Mensajería:** RabbitMQ (corriendo en Docker)
* **Librería JSON:** Jackson (con soporte para JavaTime)
* **Infraestructura:** AWS (Amazon Web Services)

## Cómo ejecutar el proyecto

### Pre-requisitos
* Tener **Docker Desktop** instalado y el contenedor de RabbitMQ activo.
* Java JDK 11 instalado.
* El servicio nativo de RabbitMQ en Windows debe estar detenido para evitar conflictos de puertos.

### Pasos
1. **Clonar el repositorio:**
   ```bash
   git clone [https://github.com/Celso33Gabriel/Proyecto_3_progra.git](https://github.com/Celso33Gabriel/Proyecto_3_progra.git)
2. Iniciar el Producer: Ejecutar la clase Main del proyecto Producer para descargar las transacciones, firmarlas y enviarlas a RabbitMQ.

3.Iniciar el Consumer: Ejecutar la clase Main del proyecto Consumer para que empiece a procesar las colas y enviar los datos a la nube.

4.Verificación: Observar la consola del Consumer para confirmar los códigos 201 OK de la API de Amazon.

Video de Explicacion:

## Alumno: Celso Gabriel Sarceño Corado

## Carnet: 0905-24-4036
