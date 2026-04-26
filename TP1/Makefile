# Makefile para compilar y levantar el sistema TP1 (macOS).
#
# Targets:
#   make            -> compila + jar + run
#   make compile    -> compila los 3 subsistemas
#   make jar        -> empaqueta los .jar en la carpeta jars/
#   make run        -> abre las 3 terminales: pantalla, puesto_atencion y terminal_registro
#   make clean      -> borra todos los bin/ y los .jar de jars/
#   make stop       -> mata los procesos java de este proyecto
#
# Requiere: macOS con Terminal.app y JDK en el PATH.

ROOT     := $(shell pwd)
JARS_DIR := $(ROOT)/jars

# subsistema:nombre_jar:clase_main
COMPONENTES := pantalla:pantalla:pantalla.Main \
               puesto_atencion:atencion:puesto_atencion.Main \
               terminal_registro:registro:terminal_registro.Main

.PHONY: all compile jar run clean stop \
        run-pantalla run-atencion run-registro

all: compile jar run

# ---------- COMPILACION ----------

compile:
	@for c in $(COMPONENTES); do \
	  sub=$${c%%:*}; \
	  echo "==> Compilando $$sub"; \
	  ( cd "$(ROOT)/$$sub" && rm -rf bin && mkdir bin \
	    && find src -name "*.java" -print0 | xargs -0 javac -d bin -sourcepath src ) \
	    || exit 1; \
	done
	@echo "==> Compilacion OK"

# ---------- EMPAQUETADO DE JARS ----------

jar: compile
	@mkdir -p "$(JARS_DIR)"
	@for c in $(COMPONENTES); do \
	  sub=$${c%%:*}; rest=$${c#*:}; \
	  jarname=$${rest%%:*}; main=$${rest#*:}; \
	  echo "==> Empaquetando $$jarname.jar"; \
	  ( cd "$(ROOT)/$$sub" && jar cfe "$(JARS_DIR)/$$jarname.jar" "$$main" -C bin . ) \
	    || exit 1; \
	done
	@echo "==> JARs generados en $(JARS_DIR)"

# ---------- LEVANTAR EL SISTEMA ----------

run: jar run-pantalla run-atencion run-registro
	@echo "==> Sistema levantado. 3 terminales abiertas."

run-pantalla:
	@osascript \
	    -e 'tell application "Terminal"' \
	    -e '  activate' \
	    -e "  do script \"cd $(ROOT) && echo '[PANTALLA]' && java -jar jars/pantalla.jar\"" \
	    -e 'end tell' >/dev/null
	@sleep 1

run-atencion:
	@osascript \
	    -e 'tell application "Terminal"' \
	    -e '  activate' \
	    -e "  do script \"cd $(ROOT) && echo '[PUESTO]' && java -jar jars/atencion.jar\"" \
	    -e 'end tell' >/dev/null
	@sleep 1

run-registro:
	@osascript \
	    -e 'tell application "Terminal"' \
	    -e '  activate' \
	    -e "  do script \"cd $(ROOT) && echo '[REGISTRO]' && java -jar jars/registro.jar\"" \
	    -e 'end tell' >/dev/null
	@sleep 1

# ---------- LIMPIEZA ----------

clean:
	@for c in $(COMPONENTES); do \
	  sub=$${c%%:*}; \
	  echo "==> Limpiando $$sub/bin"; \
	  rm -rf "$(ROOT)/$$sub/bin"; \
	done
	@echo "==> Limpiando $(JARS_DIR)/*.jar"
	@rm -f "$(JARS_DIR)"/*.jar

stop:
	@echo "==> Matando procesos java de este proyecto"
	@pkill -f "java -jar jars/pantalla.jar"  || true
	@pkill -f "java -jar jars/atencion.jar"  || true
	@pkill -f "java -jar jars/registro.jar"  || true
