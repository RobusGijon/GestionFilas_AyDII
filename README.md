# Sistema de Gestion Digital de Filas

Trabajos practicos de la asignatura **Analisis y Diseno de Sistemas II** (Universidad Nacional de Mar del Plata, Facultad de Ingenieria, Ingenieria en Informatica - Grupo 5, 2026).

## Organizacion del repositorio

```
.
├── TP1/   # MVP P2P: terminal_registro, puesto_atencion y pantalla comunicandose por Sockets TCP
└── TP2/   # Evolucion del sistema con servidor_central, multiples puestos y terminales
```

Cada TP es independiente: tiene su propio codigo fuente, sus subsistemas y su `Makefile` con targets para compilar, empaquetar y ejecutar.

### TP1

MVP con un solo puesto de atencion. Tres subsistemas:

- `terminal_registro/` - el cliente ingresa su DNI.
- `puesto_atencion/` - el operador llama al siguiente cliente.
- `pantalla/` - monitor de sala con turno actual e historial.

### TP2

Extension del sistema con un servidor central que coordina la fila y permite **N puestos de atencion** y **M terminales de registro** en paralelo. Cuatro subsistemas:

- `servidor_central/` - coordina la fila, los puestos y la pantalla.
- `puesto_atencion/`, `terminal_registro/`, `pantalla/` - clientes del servidor.

## Ejecucion (macOS)

Cada TP se levanta desde su propia carpeta con `make`. Targets comunes en ambos:

- `make compile` - compila los subsistemas.
- `make jar` - genera los `.jar` en `jars/`.
- `make run` - abre una terminal por componente y los ejecuta.
- `make all` - `compile + jar + run`.
- `make clean` - borra `bin/` y los `.jar`.
- `make stop` - mata los procesos java del proyecto.

### TP1

```bash
cd TP1
make run
```

Levanta 3 terminales: pantalla, puesto_atencion y terminal_registro.

### TP2

```bash
cd TP2
make run                          # 1 servidor + 1 pantalla + 4 puestos + 4 registros
make run PUESTOS=2 REGISTROS=6    # cantidades configurables
```

## Integrantes

- Robustiano Gijon
- Ariel Rodriguez
- Bautista Joaquin Aliende
