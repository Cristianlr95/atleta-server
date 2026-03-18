# Documento de Requisitos

## Introducción

Este documento define los requisitos para desarrollar una API REST completa para la gestión de atletas y fútbol. La API debe manejar atletas, perfiles de jugadores, equipos, partidos, estadísticas y todo el ecosistema relacionado con la organización de fútbol amateur y profesional.

## Glosario

- **API_Atletas**: El sistema de API REST para gestión de atletas y fútbol
- **Athlete**: Entidad principal que representa la identidad global de un atleta
- **Player_Profile**: Perfil específico de fútbol asociado a un atleta
- **Team**: Equipo de fútbol con sus miembros y estadísticas
- **Match**: Partido de fútbol con equipos, jugadores y eventos
- **Position**: Posición de juego (Portero, Defensa, Mediocampista, etc.)
- **Trust_Score**: Puntuación de confianza del jugador basada en su comportamiento
- **Match_Event**: Eventos durante un partido (goles, asistencias)
- **Player_History**: Registro inmutable del historial de partidos de un jugador

## Requisitos

### Requisito 1: Gestión de Atletas

**Historia de Usuario:** Como administrador del sistema, quiero gestionar atletas con identidad global única, para mantener un registro centralizado de todos los participantes.

#### Criterios de Aceptación

1. CUANDO se registre un atleta, EL Sistema DEBERÁ crear un UUID único como identificador principal
2. CUANDO se registre un atleta, EL Sistema DEBERÁ validar que el email sea único en todo el sistema
3. EL Sistema DEBERÁ almacenar de forma segura el hash de la contraseña del atleta
4. CUANDO se cree un atleta, EL Sistema DEBERÁ registrar automáticamente la fecha de creación
5. EL Sistema DEBERÁ validar que el nombre del atleta no esté vacío y tenga formato válido

### Requisito 2: Perfiles de Jugadores de Fútbol

**Historia de Usuario:** Como atleta, quiero tener un perfil específico de fútbol, para participar en equipos y partidos con mi información deportiva.

#### Criterios de Aceptación

1. CUANDO se cree un perfil de jugador, EL Sistema DEBERÁ asociarlo a un atleta existente
2. EL Sistema DEBERÁ asignar un trust_score inicial de 100 a cada nuevo perfil
3. CUANDO se cree un perfil, EL Sistema DEBERÁ permitir un alias único para el contexto de fútbol
4. EL Sistema DEBERÁ mantener la relación uno-a-uno entre atleta y perfil de jugador
5. CUANDO se actualice el trust_score, EL Sistema DEBERÁ registrar el cambio en trust_logs

### Requisito 3: Gestión de Posiciones y Preferencias

**Historia de Usuario:** Como jugador, quiero definir mis posiciones preferidas con prioridades, para que los equipos conozcan mis capacidades y preferencias.

#### Criterios de Aceptación

1. EL Sistema DEBERÁ mantener un catálogo fijo de posiciones (Portero, Defensa, Carrilero, Mediocampista, Delantero, DT)
2. CUANDO un jugador defina posiciones, EL Sistema DEBERÁ permitir asignar prioridades (1, 2, 3)
3. EL Sistema DEBERÁ mantener un contador de experiencia (XP) por posición para cada jugador
4. CUANDO se asignen posiciones, EL Sistema DEBERÁ validar que las prioridades sean únicas por jugador
5. EL Sistema DEBERÁ permitir que un jugador tenga múltiples posiciones con diferentes prioridades

### Requisito 4: Gestión de Equipos

**Historia de Usuario:** Como organizador, quiero crear y gestionar equipos de fútbol, para organizar partidos y competencias.

#### Criterios de Aceptación

1. CUANDO se cree un equipo, EL Sistema DEBERÁ asignar un creador responsable del equipo
2. EL Sistema DEBERÁ permitir almacenar información básica del equipo (nombre, logo, año de fundación)
3. CUANDO se cree un equipo, EL Sistema DEBERÁ inicializar automáticamente las estadísticas en cero
4. EL Sistema DEBERÁ validar que el nombre del equipo sea único
5. CUANDO se gestione un equipo, EL Sistema DEBERÁ mantener la lista de miembros activos

### Requisito 5: Membresía de Equipos

**Historia de Usuario:** Como jugador, quiero unirme a equipos con roles específicos, para participar en partidos organizados.

#### Criterios de Aceptación

1. CUANDO un jugador se una a un equipo, EL Sistema DEBERÁ asignar un rol (JUGADOR, CAPITAN, DT)
2. EL Sistema DEBERÁ permitir que un jugador esté en múltiples equipos simultáneamente
3. CUANDO se gestione la membresía, EL Sistema DEBERÁ mantener el estado activo/inactivo
4. EL Sistema DEBERÁ registrar la fecha de ingreso al equipo
5. CUANDO se cambie el estado de un miembro, EL Sistema DEBERÁ mantener el historial

### Requisito 6: Gestión de Partidos

**Historia de Usuario:** Como organizador, quiero crear y gestionar partidos de fútbol, para coordinar encuentros entre equipos.

#### Criterios de Aceptación

1. CUANDO se cree un partido, EL Sistema DEBERÁ especificar la modalidad (5v5, 6v6, 7v7)
2. EL Sistema DEBERÁ permitir programar partidos con fecha, hora y ubicación (latitud, longitud)
3. CUANDO se cree un partido, EL Sistema DEBERÁ asignar un estado inicial de 'CREADO'
4. EL Sistema DEBERÁ permitir definir una cuota económica para el partido
5. CUANDO se gestione un partido, EL Sistema DEBERÁ permitir cambios de estado (CREADO, INICIADO, FINALIZADO, INVALIDO)

### Requisito 7: Participación en Partidos

**Historia de Usuario:** Como jugador, quiero confirmar mi participación en partidos, para que los organizadores conozcan la disponibilidad.

#### Criterios de Aceptación

1. CUANDO un jugador se registre para un partido, EL Sistema DEBERÁ asociarlo a un equipo específico
2. EL Sistema DEBERÁ permitir que el jugador especifique su posición para ese partido
3. CUANDO se registre la participación, EL Sistema DEBERÁ requerir confirmación del jugador
4. EL Sistema DEBERÁ asignar roles específicos para el partido (JUGADOR, CAPITAN, DT)
5. CUANDO se confirme la participación, EL Sistema DEBERÁ actualizar la lista de jugadores del partido

### Requisito 8: Eventos de Partido

**Historia de Usuario:** Como participante de un partido, quiero registrar eventos importantes, para mantener estadísticas precisas del juego.

#### Criterios de Aceptación

1. CUANDO ocurra un evento, EL Sistema DEBERÁ permitir registrar goles y asistencias
2. EL Sistema DEBERÁ requerir confirmación tanto del equipo local como visitante para eventos
3. CUANDO se registre un gol, EL Sistema DEBERÁ permitir asociar un asistente opcional
4. EL Sistema DEBERÁ mantener la trazabilidad de quién registró cada evento
5. CUANDO se confirme un evento, EL Sistema DEBERÁ actualizar automáticamente las estadísticas

### Requisito 9: Historial de Jugadores

**Historia de Usuario:** Como jugador, quiero que se mantenga un historial inmutable de mis participaciones, para consultar mi rendimiento histórico.

#### Criterios de Aceptación

1. CUANDO termine un partido, EL Sistema DEBERÁ crear registros inmutables del historial
2. EL Sistema DEBERÁ registrar estadísticas individuales (goles, asistencias) por partido
3. CUANDO se complete un partido, EL Sistema DEBERÁ calcular y asignar XP ganada
4. EL Sistema DEBERÁ registrar el resultado del partido para cada jugador
5. CUANDO se cree el historial, EL Sistema DEBERÁ incluir la posición jugada

### Requisito 10: Sistema de Confianza

**Historia de Usuario:** Como administrador, quiero gestionar la confianza de los jugadores, para mantener la calidad y fair play en los partidos.

#### Criterios de Aceptación

1. CUANDO cambie el trust_score de un jugador, EL Sistema DEBERÁ registrar el cambio en trust_logs
2. EL Sistema DEBERÁ permitir asociar cambios de confianza a partidos específicos
3. CUANDO se registre un cambio, EL Sistema DEBERÁ incluir el motivo del cambio
4. EL Sistema DEBERÁ mantener un historial completo de todos los cambios de confianza
5. CUANDO se calcule la confianza, EL Sistema DEBERÁ actualizar automáticamente el perfil del jugador