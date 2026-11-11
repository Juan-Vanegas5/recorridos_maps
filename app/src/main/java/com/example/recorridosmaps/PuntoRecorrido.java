package com.example.recorridosmaps;

import java.util.ArrayList;
import java.util.List;

/**
 * Laboratorio Google Maps: punto (latitud, longitud) que se visita en un recorrido.
 * Puede ser un docente que ofrece tutoría o un estudiante que la busca.
 */
public class PuntoRecorrido {

    public String nombre;
    public String detalle;
    public double latitud;
    public double longitud;

    public PuntoRecorrido(String nombre, String detalle, double latitud, double longitud) {
        this.nombre = nombre;
        this.detalle = detalle;
        this.latitud = latitud;
        this.longitud = longitud;
    }

    public String coordenadas() {
        return latitud + "," + longitud;
    }

    @Override
    public String toString() {
        return nombre + "\n" + detalle;
    }

    /** Docentes que ofrecen tutoría y el lugar donde atienden (Bogotá). */
    public static List<PuntoRecorrido> docentesTutores() {
        List<PuntoRecorrido> lista = new ArrayList<>();
        lista.add(new PuntoRecorrido("Docente tutor - Cálculo Integral", "Biblioteca Universidad Piloto (Chapinero)", 4.632700, -74.064800));
        lista.add(new PuntoRecorrido("Docente tutor - Programación", "Biblioteca Virgilio Barco (Teusaquillo)", 4.657600, -74.089600));
        lista.add(new PuntoRecorrido("Docente tutor - Física", "Biblioteca Luis Ángel Arango (Centro)", 4.597200, -74.073300));
        lista.add(new PuntoRecorrido("Docente tutor - Bases de Datos", "Biblioteca El Tintal (Kennedy)", 4.642000, -74.155000));
        lista.add(new PuntoRecorrido("Docente tutor - Redes", "Biblioteca Julio Mario Santo Domingo (Usaquén)", 4.733900, -74.023700));
        lista.add(new PuntoRecorrido("Docente tutor - Estadística", "Biblioteca El Tunal (Tunjuelito)", 4.576200, -74.131000));
        return lista;
    }

    /** Estudiantes que buscan tutoría y su ubicación (Bogotá). */
    public static List<PuntoRecorrido> estudiantesSolicitantes() {
        List<PuntoRecorrido> lista = new ArrayList<>();
        lista.add(new PuntoRecorrido("Estudiante - Cálculo Integral", "Chapinero", 4.648600, -74.062800));
        lista.add(new PuntoRecorrido("Estudiante - Programación", "Teusaquillo", 4.638000, -74.079000));
        lista.add(new PuntoRecorrido("Estudiante - Física", "Usaquén", 4.695000, -74.031000));
        lista.add(new PuntoRecorrido("Estudiante - Bases de Datos", "Suba", 4.741000, -74.084000));
        lista.add(new PuntoRecorrido("Estudiante - Redes", "Kennedy", 4.628000, -74.150000));
        lista.add(new PuntoRecorrido("Estudiante - Estadística", "Fontibón", 4.678000, -74.141000));
        lista.add(new PuntoRecorrido("Estudiante - Programación", "Engativá", 4.707000, -74.113000));
        lista.add(new PuntoRecorrido("Estudiante - Cálculo Integral", "Bosa", 4.619000, -74.187000));
        return lista;
    }
}
