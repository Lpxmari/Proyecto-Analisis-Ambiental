# Proyecto Estacion Monitoreo Ambiental

## 1. Descripción del Proyecto

Desarrollo de una Estacion Monitoreo Ambiental para el análisis práctico de infraestructura computacional embebida, enfocándose en el monitoreo de recursos del sistema, comunicación por buses de datos y optimización de rendimiento.

**Objetivo:** Implementar un sistema IoT que permita estudiar y analizar el comportamiento de la infraestructura computacional en dispositivos embebidos.

## 2. Alcance Simplificado

### Incluye:

- Sistema embebido con ESP32, Arduino o Raspberry Pico W
- 3 sensores básicos: DHT22 (temp/humedad), MQ-135 (calidad aire), ML8511 (UV) o GUVA-S12SD.
- Monitoreo de recursos del sistema (RAM, CPU, buses)
- Comunicación WiFi básica
- Dashboard web simple

### No incluye:

- Aplicaciones móviles
- Múltiples dispositivos
- Sistemas de respaldo complejos
- Carcasas industriales

## 3. Requisitos Funcionales de Infraestructura

### 3.1 Monitoreo de Recursos del Sistema

- **RF-001:** Monitorear uso de memoria RAM en tiempo real (heap disponible/usado)
- **RF-002:** Medir uso de procesador (% CPU, frecuencia actual)
- **RF-003:** Registrar temperatura interna del microcontrolador
- **RF-004:** Monitorear voltaje de alimentación y consumo energético
- **RF-005:** Detectar y registrar reinicios del sistema y errores de memoria

### 3.2 Comunicación por Buses de Datos

- **RF-006:** Implementar comunicación I2C con los sensores
- **RF-007:** Monitorear velocidad y latencia de transmisión I2C
- **RF-008:** Implementar comunicación SPI para almacenamiento local
- **RF-009:** Monitorear tráfico de datos por WiFi (bytes enviados/recibidos)
- **RF-010:** Registrar errores de comunicación y reintentos

### 3.3 Medición de Sensores

- **RF-011:** Leer temperatura y humedad cada 30 segundos
- **RF-012:** Medir calidad del aire cada 120 segundos
- **RF-013:** Registrar intensidad UV cada 120 segundos
- **RF-014:** Validar lecturas y detectar sensores desconectados

### 3.4 Transmisión de Datos

- **RF-015:** Enviar datos cada 15 minutos vía WiFi
- **RF-016:** Incluir métricas de sistema en cada transmisión
- **RF-017:** Implementar buffer local para 20 lecturas

## 4. Especificaciones Técnicas de Infraestructura

### 4.1 Microcontrolador/ Microprocesador

**Recursos del Sistema:**

- **RAM Objetivo:** Uso máximo 60%
- **CPU:** Depende del hardware elegido (se requiere configuración para ahorro energético)
- **Flash:** 4 MB para código y datos
- **Monitoreo:** Cada 30 segundos

### 4.2 Buses de Comunicación

**Bus I2C:**

- **Velocidad:** 100 kHz (modo estándar)
- **Pines:** GPIO 21 (SDA), GPIO 22 (SCL)
- **Dispositivos:** sensores requeridos
- **Latencia esperada:** <5ms por lectura
- **Monitoreo:** Tiempo de transacción, errores, reintentos

**Bus SPI (opcional):**

- **Velocidad:** 1 MHz aproximadamente (depende del sistema de almacenamiento elegido)
- **Uso:** Almacenamiento local de logs
- **Monitoreo:** Throughput, errores de escritura

**WiFi:**

- **Protocolo:** 802.11 b/g/n
- **Velocidad:** Hasta 150 Mbps
- **Uso real esperado:** 1-5 KB cada 5 minutos
- **Monitoreo:** RSSI, bytes TX/RX, reconexiones

### 4.3 Consumo de Recursos Estimado (Ejemplo)

**Memoria RAM:**

```
- Sistema base ESP32:     ~50 KB
- WiFi stack:             ~40 KB
- Buffers de sensores:    ~5 KB
- Buffer de transmisión:  ~10 KB
- Variables y estructuras: ~15 KB
- Heap libre mínimo:      ~400 KB
Total estimado: 120 KB (23% de 520 KB)
```

**Uso de CPU (Ejemplo):**

```
- Lectura de sensores:    5% (cada 30-60s)
- Procesamiento de datos: 2% continuo
- Transmisión WiFi:      10% (durante 2-3s cada 5min)
- Monitoreo de sistema:   3% continuo
- Sistema idle:          80% promedio

```

## 5. Arquitectura del Sistema

### 5.1 Hardware

- **Microcontrolador/Microprocesador:**
- **Sensores:**
    - DHT22: Temperatura/Humedad
    - MQ-135: Calidad del aire
    - Sensor Intensidad UV
- **Alimentación:** USB 5V / 3.3V regulado
- **Almacenamiento:** Tarjeta micro SD para logs locales

### 5.2 Software

- **Framework:** Arduino Core para ESP32
- **Librerías principales** (Ejemplo)**:**
    - WiFi.h (conectividad)
    - DHT.h (sensor temperatura)
    - ArduinoJson.h (formateo datos)
    - ESP.h (monitoreo sistema)

## 6. Plan de Implementación

### Configuración Base

- Configurar entorno de desarrollo
- Implementar lectura básica de sensores
- Establecer comunicación I2C

### Monitoreo de Sistema

- Desarrollar funciones de monitoreo de RAM y CPU
- Implementar logging de métricas de sistema
- Probar comunicación por buses

### Conectividad y Transmisión

- Configurar conectividad WiFi
- Desarrollar protocolo de transmisión de datos
- Incluir métricas de infraestructura en payload

### Optimización y Validación

- Optimizar uso de recursos
- Validar rendimiento del sistema
- Documentar métricas obtenidas

## 7. Métricas de Evaluación

### Rendimiento del Sistema:

- Uso máximo de RAM durante operación
- Porcentaje promedio de uso de CPU
- Tiempo de respuesta de sensores I2C
- Latencia de transmisión WiFi
- Número de reinicios en 24 horas

### Objetivos de Rendimiento:

- RAM utilizada < 60% del total disponible
- CPU idle > 75% del tiempo
- Transmisión I2C < 10ms por sensor
- Conectividad WiFi > 95% uptime
- Cero reinicios inesperados del sistema

## 8. Entregables

1. **Código fuente** documentado con métricas de sistema
2. **Reporte de análisis** de infraestructura computacional
3. **Dashboard básico** mostrando métricas de sistema y sensores
4. **Documentación técnica** de buses de comunicación y rendimiento
5. **Análisis de optimización** de recursos del sistema