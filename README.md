# Sistema de Gestion Digital de Filas

Trabajos practicos de la asignatura **Analisis y Diseno de Sistemas II** (Universidad Nacional de Mar del Plata, Facultad de Ingenieria, Ingenieria en Informatica - Grupo 5, 2026).

## Organizacion del repositorio

```
.
├── TP1/   # MVP P2P: terminal_registro, puesto_atencion y pantalla comunicandose por Sockets TCP
├── TP2/   # Evolucion del sistema con servidor_central, multiples puestos y terminales
├── TP3/   # Disponibilidad: servidor_central redundante, retry, heartbeats, recuperacion
└── TP4/   # Persistencia (JSON/XML/TXT) + seguridad: cifrado simetrico del DNI en transito y en disco
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

### TP4

Sobre la base del TP3 (que se mantiene: redundancia, heartbeats y failover) se agregan **persistencia local** y **seguridad en la comunicacion**, eliminando la restriccion de que los datos vivieran solo en RAM.

**Persistencia** (en el servidor central): ante un reinicio o caida, el primario recupera de disco la cola de espera, el historial de llamados de la pantalla y los intentos de re-notificacion de los puestos. El formato es configurable (`--formato json|xml|txt`). El estado se guarda en un unico archivo por combinacion cifrado+formato (p.ej. `data/estado.aes.json` o `data/estado.plano.txt`), **compartido** a proposito entre primario y secundario para que sobreviva al failover, y se escribe de forma **atomica** para evitar lecturas parciales. Solo el nodo activo persiste.

**Seguridad**: el DNI viaja y se almacena cifrado con un esquema **simetrico**. El metodo se elige por configuracion (`AES`, `XOR`, `CESAR` o `VIGENERE`) y la **clave secreta** se lee de `shared/.env` (variable `<METODO>_KEY`). Todos los componentes (servidor, puestos, terminales y pantalla) deben usar el mismo metodo y clave para entenderse.

Patrones de diseno GoF aplicados:

| Patron           | Donde                                                                                               |
| ---------------- | --------------------------------------------------------------------------------------------------- |
| Abstract Factory | `IFabricaPersistencia` (`FabricaJSON/XML/TXT`) crea la familia escritor + lector                    |
| Strategy         | `IEstrategiaEncriptacion` con las estrategias `EncriptacionAES/XOR/Cesar/Vigenere`                  |
| Factory Method   | `FabricaPersistenciaFactory.crear(formato)` selecciona la fabrica concreta                          |
| Template Method  | `CanalMensajes` define el flujo y los hooks `alCable/delCable` que cifra `CanalMensajesEncriptados` |
| Observer         | `IObservadorReplicacion` y los publicadores hacia pantalla/puestos                                  |
| Facade           | `PersistenciaService` expone `guardar/leer` ocultando escritor y lector                             |

- `servidor_central/` - suma los flags `--encrip AES|XOR|CESAR|VIGENERE` y `--formato json|xml|txt`. Sin `--encrip` persiste y comunica en claro; sin `--formato` usa `txt`.
- `puesto_atencion/`, `terminal_registro/`, `pantalla/` - reciben el metodo de cifrado como argumento y leen la clave del `.env` compartido.

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
| ------------ | ----------- | -------------------------- |
| `IP_PRI`     | `127.0.0.1` | IP del servidor primario   |
| `PUERTO_PRI` | `8080`      | Puerto del primario        |
| `IP_SEC`     | `127.0.0.1` | IP del servidor secundario |
| `PUERTO_SEC` | `8081`      | Puerto del secundario      |

Targets nuevos:

- `make run-servidor-pri` - solo levanta el primario con los args actuales.
- `make run-servidor-sec` - solo levanta el secundario con los args actuales.
- `make run-servidor` - los dos en orden (secundario primero, luego primario).

Targets de simulacion de caida (para probar el failover de alta disponibilidad):

- `make kill-pri` - mata el nodo primario con `SIGKILL` (caida abrupta, sin cleanup); el secundario lo detecta por timeout de heartbeat y se autopromueve.
- `make kill-sec` - igual para el secundario.
- `make stop` - baja prolijamente (`SIGTERM`) todos los procesos java del proyecto.

Para un despliegue en dos maquinas distintas no usar `make run` (intentaria levantar ambos nodos en la misma): correr el target por nodo en cada maquina.

```bash
# Maquina A - primario:
make run-servidor-pri IP_PRI=192.168.1.10 IP_SEC=192.168.1.11
# Maquina B - secundario:
make run-servidor-sec IP_PRI=192.168.1.10 IP_SEC=192.168.1.11
```

### TP4

Misma estructura de `make run` que el TP3 (primario + secundario + pantalla + N puestos + M registros), con dos variables nuevas para cifrado y formato de persistencia.

```bash
cd TP4
make run                          # sin cifrar, persistencia en json
make run ENCRIP=AES               # cifra DNI con AES (la clave esta en shared/.env -> AES_KEY)
make run ENCRIP=AES FORMATO=xml   # cifrado + formato de persistencia configurables
```

Variables nuevas del Makefile

| Variable  | Default | Descripcion                                                               |
| --------- | ------- | ------------------------------------------------------------------------- |
| `ENCRIP`  | (vacio) | Metodo de cifrado: `AES`, `XOR`, `CESAR` o `VIGENERE`. Vacio = sin cifrar |
| `FORMATO` | `json`  | Formato de persistencia del servidor: `json`, `xml` o `txt`               |

El mismo `ENCRIP` se aplica a **todos** los componentes (servidor, puestos, registros y pantalla) para que compartan metodo y clave.

## Integrantes

- Robustiano Gijon
- Ariel Rodriguez
- Bautista Joaquin Aliende
