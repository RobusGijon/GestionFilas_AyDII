# Sistema de Gestion Digital de Filas

Trabajos practicos de la asignatura **Analisis y Diseno de Sistemas II** (Universidad Nacional de Mar del Plata, Facultad de Ingenieria, Ingenieria en Informatica - Grupo 5, 2026).

## Organizacion del repositorio

```
.
├── TP1/   # MVP P2P: terminal_registro, puesto_atencion y pantalla comunicandose por Sockets TCP
├── TP2/   # Evolucion del sistema con servidor_central, multiples puestos y terminales
└── TP3/   # Disponibilidad: servidor_central redundante, retry, heartbeats, recuperacion 
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

### TP3

Se agrego la disponibilidad sobre la base del TP2: el servidor central pasa a operar como un par de nodos **primario** (acepta clientes) + **secundario** (replica caliente). El primario replica cada cambio al secundario y emite heartbeats; si cae, el secundario se autopromueve y los clientes hacen failover automatico al segundo nodo. Tiempo de failover percibido: **≤ 5 s**.

- `servidor_central/` - ahora acepta los flags `--puerto`, `--rol primario|secundario` y `--par ip:puerto`. Sin argumentos arranca en modo TP2 (un solo nodo).
- `puesto_atencion/`, `terminal_registro/`, `pantalla/` - configuran dos pares IP/puerto (primario y secundario) y reintentan con backoff antes de hacer failover.

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

### TP3

`make run` ahora abre **2 terminales de servidor** (primario + secundario) ademas de pantalla, puestos y registros, levantando cada nodo con sus flags `--puerto / --rol / --par`.

```bash
cd TP3
make run                          # primario + secundario + 1 pantalla + 4 puestos + 4 registros
make run PUERTO_PRI=9000 PUERTO_SEC=9001   # puertos configurables
make run PUESTOS=2 REGISTROS=1             # cantidades configurables (igual que TP2)
```

Variables nuevas del Makefile (override desde la linea de comandos):

| Variable     | Default     | Descripcion                |
|--------------|-------------|----------------------------|
| `IP_PRI`     | `127.0.0.1` | IP del servidor primario   |
| `PUERTO_PRI` | `8080`      | Puerto del primario        |
| `IP_SEC`     | `127.0.0.1` | IP del servidor secundario |
| `PUERTO_SEC` | `8081`      | Puerto del secundario      |

Targets nuevos:

- `make run-servidor-pri` - solo levanta el primario con los args actuales.
- `make run-servidor-sec` - solo levanta el secundario con los args actuales.
- `make run-servidor` - los dos en orden (secundario primero, luego primario).

Para un despliegue en dos maquinas distintas no usar `make run` (intentaria levantar ambos nodos en la misma): correr el target por nodo en cada maquina.

```bash
# Maquina A - primario:
make run-servidor-pri IP_PRI=192.168.1.10 IP_SEC=192.168.1.11
# Maquina B - secundario:
make run-servidor-sec IP_PRI=192.168.1.10 IP_SEC=192.168.1.11
```

## Integrantes

- Robustiano Gijon
- Ariel Rodriguez
- Bautista Joaquin Aliende
