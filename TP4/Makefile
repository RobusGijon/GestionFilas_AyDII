# Makefile para compilar y levantar el sistema completo (macOS).
#
# Targets:
#   make            -> compila los 4 subsistemas
#   make jar        -> empaqueta los .jar en la carpeta jars/
#   make run        -> genera los .jar y abre las terminales:
#                       2 servidores (primario + secundario) + 1 pantalla
#                       + N puestos + M registros
#   make all        -> compila + jar + run
#   make compile    -> compila los 4 subsistemas
#   make clean      -> borra todos los bin/ y los .jar de jars/
#   make stop       -> mata todos los java de este proyecto (prolijo, SIGTERM)
#   make kill-pri   -> simula CAIDA ABRUPTA del nodo primario  (SIGKILL, sin cleanup)
#   make kill-sec   -> simula CAIDA ABRUPTA del nodo secundario (SIGKILL, sin cleanup)
#
# Variables configurables (opcionales):
#   IP_PRI      -> IP del servidor primario   (default 127.0.0.1)
#   PUERTO_PRI  -> puerto del primario        (default 8080)
#   IP_SEC      -> IP del servidor secundario (default 127.0.0.1)
#   PUERTO_SEC  -> puerto del secundario      (default 8081)
#   PUESTOS     -> cantidad de puestos        (default 2)
#   REGISTROS   -> cantidad de terminales     (default 2)
#   ENCRIP      -> metodo de encriptacion     (default vacio = sin cifrar)
#                  valores: AES|XOR|CESAR|VIGENERE. La clave se lee de
#                  shared/.env en la variable <METODO>_KEY.
#   FORMATO     -> formato de persistencia     (default json)
#                  valores: json|xml|txt. Lo usa el servidor central.
#
# Ejemplos:
#   make run
#   make run PUERTO_PRI=9000 PUERTO_SEC=9001
#   make run IP_PRI=192.168.1.10 IP_SEC=192.168.1.11 PUESTOS=2 REGISTROS=2
#   make run ENCRIP=AES

ROOT       := $(shell pwd)
JARS_DIR   := $(ROOT)/jars
SUBSISTEMAS := servidor_central puesto_atencion pantalla terminal_registro

IP_PRI     ?= 127.0.0.1
PUERTO_PRI ?= 8080
IP_SEC     ?= 127.0.0.1
PUERTO_SEC ?= 8081

PUESTOS    ?= 2
REGISTROS  ?= 2

# Metodo de encriptacion aplicado a TODOS los componentes (mismo metodo y clave).
# Vacio = sin encriptacion. La clave se lee de shared/.env (<METODO>_KEY).
ENCRIP     ?=

# Formato de persistencia del servidor central (json|xml|txt). Primario y
# secundario usan el mismo. Default json.
FORMATO    ?= json

# El servidor recibe el metodo via flag --encrip; vacio si no hay metodo.
ifeq ($(strip $(ENCRIP)),)
ENCRIP_FLAG :=
else
ENCRIP_FLAG := --encrip $(ENCRIP)
endif

.PHONY: all compile jar run clean stop \
        run-servidor run-servidor-pri run-servidor-sec \
        run-pantalla run-puestos run-registros \
        kill-pri kill-sec

all: compile jar run

# ---------- COMPILACION ----------

compile:
	@for sub in $(SUBSISTEMAS); do \
	  echo "==> Compilando $$sub"; \
	  ( cd "$(ROOT)/$$sub" && rm -rf bin && mkdir bin \
	    && find src -name "*.java" -print0 | xargs -0 javac -d bin -sourcepath src ) \
	    || exit 1; \
	done
	@echo "==> Compilacion OK"

# ---------- EMPAQUETADO DE JARS ----------

jar: compile
	@mkdir -p "$(JARS_DIR)"
	@for sub in $(SUBSISTEMAS); do \
	  echo "==> Empaquetando $$sub.jar"; \
	  ( cd "$(ROOT)/$$sub" && jar cfe "$(JARS_DIR)/$$sub.jar" "$$sub.Main" -C bin . ) \
	    || exit 1; \
	done
	@echo "==> JARs generados en $(JARS_DIR)"

# ---------- LEVANTAR EL SISTEMA ----------

run: jar run-servidor run-pantalla run-puestos run-registros
	@echo "==> Sistema levantado. $$((3 + $(PUESTOS) + $(REGISTROS))) terminales abiertas."

run-servidor: run-servidor-sec run-servidor-pri
	@echo "==> Servidores levantados: PRI=$(IP_PRI):$(PUERTO_PRI) SEC=$(IP_SEC):$(PUERTO_SEC)"

run-servidor-sec: compile
	@osascript \
	    -e 'tell application "Terminal"' \
	    -e '  activate' \
	    -e "  do script \"cd $(ROOT)/servidor_central && echo '[SECUNDARIO $(IP_SEC):$(PUERTO_SEC)]' && java -cp bin servidor_central.Main --puerto $(PUERTO_SEC) --rol secundario --par $(IP_PRI):$(PUERTO_PRI) $(ENCRIP_FLAG) --formato $(FORMATO)\"" \
	    -e 'end tell' >/dev/null
	@sleep 2

run-servidor-pri: compile
	@osascript \
	    -e 'tell application "Terminal"' \
	    -e '  activate' \
	    -e "  do script \"cd $(ROOT)/servidor_central && echo '[PRIMARIO $(IP_PRI):$(PUERTO_PRI)]' && java -cp bin servidor_central.Main --puerto $(PUERTO_PRI) --rol primario --par $(IP_SEC):$(PUERTO_SEC) $(ENCRIP_FLAG) --formato $(FORMATO)\"" \
	    -e 'end tell' >/dev/null
	@sleep 2

run-pantalla: compile
	@osascript \
	    -e 'tell application "Terminal"' \
	    -e '  activate' \
	    -e "  do script \"cd $(ROOT)/pantalla && echo '[PANTALLA]' && java -cp bin pantalla.Main $(ENCRIP)\"" \
	    -e 'end tell' >/dev/null
	@sleep 1

run-puestos: compile
	@if [ $(PUESTOS) -gt 0 ]; then \
	  for i in $$(seq 1 $(PUESTOS)); do \
	    osascript \
	      -e 'tell application "Terminal"' \
	      -e '  activate' \
	      -e "  do script \"cd $(ROOT)/puesto_atencion && echo '[PUESTO $$i]' && java -cp bin puesto_atencion.Main $(ENCRIP)\"" \
	      -e 'end tell' >/dev/null; \
	    sleep 1; \
	  done; \
	fi

run-registros: compile
	@if [ $(REGISTROS) -gt 0 ]; then \
	  for i in $$(seq 1 $(REGISTROS)); do \
	    osascript \
	      -e 'tell application "Terminal"' \
	      -e '  activate' \
	      -e "  do script \"cd $(ROOT)/terminal_registro && echo '[REGISTRO $$i]' && java -cp bin terminal_registro.Main $(ENCRIP)\"" \
	      -e 'end tell' >/dev/null; \
	    sleep 1; \
	  done; \
	fi

# ---------- LIMPIEZA ----------

clean:
	@for sub in $(SUBSISTEMAS); do \
	  echo "==> Limpiando $$sub/bin"; \
	  rm -rf "$(ROOT)/$$sub/bin"; \
	done
	@echo "==> Limpiando $(JARS_DIR)/*.jar"
	@rm -f "$(JARS_DIR)"/*.jar

stop:
	@echo "==> Matando procesos java de este proyecto"
	@pkill -f "java -cp bin servidor_central.Main"  || true
	@pkill -f "java -cp bin pantalla.Main"          || true
	@pkill -f "java -cp bin puesto_atencion.Main"   || true
	@pkill -f "java -cp bin terminal_registro.Main" || true

# ---------- SIMULACION DE CAIDA ABRUPTA (HA) ----------
# SIGKILL (-9): no se ejecuta ningun shutdown hook, el proceso muere al instante.
# Es la forma mas fiel digamos de simular un crash; el otro nodo lo detecta por timeout de
# heartbeat y promociona. Se va al nodo por su puerto.

kill-pri:
	@pkill -9 -f "servidor_central.Main --puerto $(PUERTO_PRI)" \
	  && echo "==> CAIDA SIMULADA del nodo en puerto $(PUERTO_PRI) (PRIMARIO) [SIGKILL]" \
	  || echo "==> no habia ningun nodo corriendo en puerto $(PUERTO_PRI)"

kill-sec:
	@pkill -9 -f "servidor_central.Main --puerto $(PUERTO_SEC)" \
	  && echo "==> CAIDA SIMULADA del nodo en puerto $(PUERTO_SEC) (SECUNDARIO) [SIGKILL]" \
	  || echo "==> no habia ningun nodo corriendo en puerto $(PUERTO_SEC)"
