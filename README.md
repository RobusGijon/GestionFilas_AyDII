# Sistema de Gestion Digital de Filas

## Proposito

Producto Minimo Viable (MVP) que permite gestionar de manera digital la espera y atencion de clientes en un entorno de un solo puesto de atencion. El sistema utiliza una arquitectura Peer-to-Peer (P2P) donde los componentes se comunican directamente entre si mediante Sockets TCP, sin un servidor centralizado.

El flujo del sistema se basa en la interaccion entre tres componentes:

- **Terminal de Registro**: El cliente ingresa su DNI para sumarse a la lista de espera.
- **Puesto de Atencion**: El operador visualiza la fila y llama al siguiente cliente.
- **Pantalla (Monitor de Sala)**: Muestra el cliente siendo atendido y un historial de los 4 anteriores.

## Desarrollo

El proyecto esta desarrollado en **Java** utilizando **Swing** para las interfaces graficas y **Sockets TCP** para la comunicacion entre los componentes.

### Estructura

```
proyecto/
├── terminal_registro/    # Terminal donde el cliente ingresa su DNI
├── puesto_atencion/      # Terminal del operador con la fila y boton "Llamar Siguiente"
├── pantalla/             # Monitor de sala con turno actual e historial
└── jars/                 # Ejecutables .jar de cada componente
```

### Ejecucion

```bash
java -jar jars/pantalla.jar
java -jar jars/atencion.jar
java -jar jars/registro.jar
```

### Configuracion de red

Cada componente solicita al inicio la IP y puerto de los peers con los que se comunica:

1. **Pantalla**: Se configura el puerto de escucha.
2. **Puesto de Atencion**: Se configura el puerto de escucha propio y la IP/puerto de la pantalla.
3. **Terminal de Registro**: Se configura la IP/puerto del puesto de atencion y la IP/puerto de la pantalla.

### Requerimientos funcionales implementados

- Logica FIFO: el primer cliente en registrarse es el primero en ser llamado.
- Historial dinamico: la pantalla se actualiza automaticamente al llamar un cliente, manteniendo los 4 anteriores.
- Validacion de datos: no se permiten DNIs vacios, no numericos o con longitud distinta a 8 digitos.

## Integrantes

- Robustiano Gijon
- Ariel Rodriguez
- Bautista Joaquin Aliende

Grupo 5 - 2026  
Universidad Nacional de Mar del Plata  
Facultad de Ingenieria  
Carrera: Ingenieria en Informatica  
Asignatura: Analisis y Diseno de Sistemas II
