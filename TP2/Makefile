# Makefile para compilar y levantar el sistema completo (macOS).
#
# Targets:
#   make            -> compila los 4 subsistemas
#   make jar        -> empaqueta los .jar en la carpeta jars/
#   make run        -> genera los .jar y abre las terminales: 1 servidor + 1 pantalla + N puestos + M registros
#   make all        -> compila + jar + run
#   make compile    -> compila los 4 subsistemas
#   make clean      -> borra todos los bin/ y los .jar de jars/
#   make stop       -> mata todos los java de este proyecto
#
# Variables configurables (override desde la linea de comandos):
#   PUESTOS    -> cantidad de puestos de atencion a levantar (default 4)
#   REGISTROS  -> cantidad de terminales de registro a levantar (default 4)
#
# Ejemplo:
#   make run PUESTOS=2 REGISTROS=6
#
# Requiere: macOS con Terminal.app y JDK en el PATH.

ROOT       := $(shell pwd)
JARS_DIR   := $(ROOT)/jars
SUBSISTEMAS := servidor_central puesto_atencion pantalla terminal_registro

PUESTOS    ?= 4
REGISTROS  ?= 4

.PHONY: all compile jar run clean stop \
        run-servidor run-pantalla run-puestos run-registros

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
	@echo "==> Sistema levantado. $$((2 + $(PUESTOS) + $(REGISTROS))) terminales abiertas."

run-servidor:
	@osascript \
	    -e 'tell application "Terminal"' \
	    -e '  activate' \
	    -e "  do script \"cd $(ROOT)/servidor_central && echo '[SERVIDOR]' && java -cp bin servidor_central.Main\"" \
	    -e 'end tell' >/dev/null
	@sleep 2

run-pantalla:
	@osascript \
	    -e 'tell application "Terminal"' \
	    -e '  activate' \
	    -e "  do script \"cd $(ROOT)/pantalla && echo '[PANTALLA]' && java -cp bin pantalla.Main\"" \
	    -e 'end tell' >/dev/null
	@sleep 1

run-puestos:
	@for i in $$(seq 1 $(PUESTOS)); do \
	  osascript \
	    -e 'tell application "Terminal"' \
	    -e '  activate' \
	    -e "  do script \"cd $(ROOT)/puesto_atencion && echo '[PUESTO $$i]' && java -cp bin puesto_atencion.Main\"" \
	    -e 'end tell' >/dev/null; \
	  sleep 1; \
	done

run-registros:
	@for i in $$(seq 1 $(REGISTROS)); do \
	  osascript \
	    -e 'tell application "Terminal"' \
	    -e '  activate' \
	    -e "  do script \"cd $(ROOT)/terminal_registro && echo '[REGISTRO $$i]' && java -cp bin terminal_registro.Main\"" \
	    -e 'end tell' >/dev/null; \
	  sleep 1; \
	done

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
