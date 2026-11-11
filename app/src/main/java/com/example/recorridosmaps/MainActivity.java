package com.example.recorridosmaps;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Laboratorio: planear recorrido con Google Maps (Intent).
 * - Invoca Google Maps por medio de Intents (ACTION_VIEW + paquete de Google Maps).
 * - Señala la ubicación del proyecto (Universidad Piloto de Colombia).
 * - Usa el GPS del dispositivo para conocer la ubicación actual.
 * - Busca el docente (o estudiante) más cercano y planea el recorrido más eficiente
 *   por varios puntos, con alternativas de ruta según el modo de transporte.
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "Directions";
    private static final int REQ_UBICACION = 100;
    private static final String MAPS_PACKAGE = "com.google.android.apps.maps";

    // Ubicación del proyecto: Universidad Piloto de Colombia (coordenadas de la guía)
    private static final String UPC_NOMBRE = "Universidad Piloto de Colombia";
    private static final double UPC_LAT = 4.632339710;
    private static final double UPC_LNG = -74.065350;

    // Google Maps admite máximo 9 paradas intermedias + el destino
    private static final int MAX_PUNTOS = 10;

    private static final String[] MODOS_TEXTO = {"Conduciendo", " Caminando", "Bicicleta", "Transporte público"};
    private static final String[] TIPOS_LISTA = {"Soy estudiante: buscar docente tutor", "Soy docente: visitar estudiantes que buscan tutoría"};
    private static final String[] MODOS_MAPS = {"driving", "walking", "bicycling", "transit"};

    private TextView tvUbicacionActual, tvTituloLista, tvResultadoRecorrido;
    private Spinner spinnerModoTransporte, spinnerTipoLista;
    private ListView listPuntos;

    private List<PuntoRecorrido> puntos = new ArrayList<>();
    private Location ubicacionActual;
    private LocationManager locationManager;
    private LocationListener listenerGps;
    private Button btnMasCercano;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvUbicacionActual = findViewById(R.id.tvUbicacionActual);
        tvTituloLista = findViewById(R.id.tvTituloLista);
        tvResultadoRecorrido = findViewById(R.id.tvResultadoRecorrido);
        spinnerModoTransporte = findViewById(R.id.spinnerModoTransporte);
        spinnerTipoLista = findViewById(R.id.spinnerTipoLista);
        listPuntos = findViewById(R.id.listPuntos);
        Button btnObtenerUbicacion = findViewById(R.id.btnObtenerUbicacion);
        Button btnUbicacionUniversidad = findViewById(R.id.btnUbicacionUniversidad);
        btnMasCercano = findViewById(R.id.btnMasCercano);
        Button btnPlanearRecorrido = findViewById(R.id.btnPlanearRecorrido);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        ArrayAdapter<String> adapterModos = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, MODOS_TEXTO);
        adapterModos.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerModoTransporte.setAdapter(adapterModos);

        ArrayAdapter<String> adapterTipos = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, TIPOS_LISTA);
        adapterTipos.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTipoLista.setAdapter(adapterTipos);
        spinnerTipoLista.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                cargarLista(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        listPuntos.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        listPuntos.setOnItemLongClickListener((parent, view, position, id) -> {
            PuntoRecorrido p = puntos.get(position);
            mostrarUbicacion(p.nombre, p.latitud, p.longitud);
            return true;
        });

        btnObtenerUbicacion.setOnClickListener(v -> obtenerUbicacionGPS());
        btnUbicacionUniversidad.setOnClickListener(v -> mostrarUbicacion(UPC_NOMBRE, UPC_LAT, UPC_LNG));
        btnMasCercano.setOnClickListener(v -> irAlMasCercano());
        btnPlanearRecorrido.setOnClickListener(v -> planearRecorrido());

        obtenerUbicacionGPS();
    }

    /**
     * 0: el estudiante busca al docente tutor más cercano.
     * 1: el docente recorre a los estudiantes que buscan tutoría.
     */
    private void cargarLista(int tipo) {
        if (tipo == 0) {
            puntos = PuntoRecorrido.docentesTutores();
            tvTituloLista.setText("Docentes tutores (mantén presionado para ver en el mapa):");
            btnMasCercano.setText("Docente más cercano");
        } else {
            puntos = PuntoRecorrido.estudiantesSolicitantes();
            tvTituloLista.setText("Estudiantes que buscan tutoría (mantén presionado para ver en el mapa):");
            btnMasCercano.setText("Estudiante más cercano");
        }
        listPuntos.clearChoices();
        listPuntos.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_multiple_choice, puntos));
        tvResultadoRecorrido.setText("Aquí aparece el orden del recorrido.");
    }

    // ---------------------------------------------------------------------
    // Google Maps: señalar una ubicación en coordenadas latitud/longitud
    // ---------------------------------------------------------------------
    private void mostrarUbicacion(String nombre, double lat, double lng) {
        Uri gmmIntentUri = Uri.parse("geo:" + lat + "," + lng + "?q=" + lat + "," + lng + "(" + Uri.encode(nombre) + ")");
        Log.d(TAG, gmmIntentUri.toString());
        abrirGoogleMaps(gmmIntentUri);
    }

    // ---------------------------------------------------------------------
    // Docente / estudiante más cercano: recorrido entre origen y destino
    // ---------------------------------------------------------------------
    private void irAlMasCercano() {
        double[] origen = obtenerOrigen();
        PuntoRecorrido masCercano = null;
        float menorDistancia = Float.MAX_VALUE;

        for (PuntoRecorrido p : puntos) {
            float d = distanciaMetros(origen[0], origen[1], p.latitud, p.longitud);
            if (d < menorDistancia) {
                menorDistancia = d;
                masCercano = p;
            }
        }
        if (masCercano == null) return;

        tvResultadoRecorrido.setText(String.format(Locale.getDefault(),
                "Más cercano: %s\n%s\nDistancia en línea recta: %.2f km\nGoogle Maps muestra las rutas alternativas para llegar.",
                masCercano.nombre, masCercano.detalle, menorDistancia / 1000f));

        Uri.Builder builder = new Uri.Builder();
        builder.scheme("https")
                .authority("www.google.com")
                .appendPath("maps")
                .appendPath("dir")
                .appendPath("")
                .appendQueryParameter("api", "1")
                .appendQueryParameter("origin", origen[0] + "," + origen[1])
                .appendQueryParameter("destination", masCercano.coordenadas())
                .appendQueryParameter("travelmode", modoSeleccionado());
        String url = builder.build().toString();
        Log.d(TAG, url);
        abrirGoogleMaps(Uri.parse(url));
    }

    // ---------------------------------------------------------------------
    // Recorrido diario por varias ubicaciones, en el orden más eficiente
    // (siempre se va al punto más cercano que falte por visitar)
    // ---------------------------------------------------------------------
    private void planearRecorrido() {
        List<PuntoRecorrido> pendientes = new ArrayList<>();
        SparseBooleanArray marcados = listPuntos.getCheckedItemPositions();
        for (int i = 0; i < puntos.size(); i++) {
            if (marcados != null && marcados.get(i)) pendientes.add(puntos.get(i));
        }
        if (pendientes.isEmpty()) {
            Toast.makeText(this, "Selecciona al menos un punto de la lista", Toast.LENGTH_SHORT).show();
            return;
        }
        if (pendientes.size() > MAX_PUNTOS) {
            Toast.makeText(this, "Google Maps permite máximo " + MAX_PUNTOS + " puntos por recorrido", Toast.LENGTH_LONG).show();
            return;
        }

        String modo = modoSeleccionado();
        if ("transit".equals(modo) && pendientes.size() > 1) {
            Toast.makeText(this, "Transporte público no admite paradas intermedias; se usa conducción", Toast.LENGTH_LONG).show();
            modo = "driving";
        }

        double[] origen = obtenerOrigen();
        double latActual = origen[0], lngActual = origen[1];
        List<PuntoRecorrido> orden = new ArrayList<>();
        StringBuilder resumen = new StringBuilder("Orden óptimo del recorrido:\n");
        float total = 0;

        while (!pendientes.isEmpty()) {
            PuntoRecorrido siguiente = pendientes.get(0);
            float menor = Float.MAX_VALUE;
            for (PuntoRecorrido p : pendientes) {
                float d = distanciaMetros(latActual, lngActual, p.latitud, p.longitud);
                if (d < menor) {
                    menor = d;
                    siguiente = p;
                }
            }
            pendientes.remove(siguiente);
            orden.add(siguiente);
            total += menor;
            resumen.append(String.format(Locale.getDefault(), "%d. %s (%s) - %.2f km\n",
                    orden.size(), siguiente.nombre, siguiente.detalle, menor / 1000f));
            latActual = siguiente.latitud;
            lngActual = siguiente.longitud;
        }
        resumen.append(String.format(Locale.getDefault(), "Total aproximado (línea recta): %.2f km", total / 1000f));
        tvResultadoRecorrido.setText(resumen.toString());

        // Definir origen, destino y paradas intermedias (waypoints)
        StringBuilder waypoints = new StringBuilder();
        for (int i = 0; i < orden.size() - 1; i++) {
            if (waypoints.length() > 0) waypoints.append("|");
            waypoints.append(orden.get(i).coordenadas());
        }

        Uri.Builder builder = new Uri.Builder();
        builder.scheme("https")
                .authority("www.google.com")
                .appendPath("maps")
                .appendPath("dir")
                .appendPath("")
                .appendQueryParameter("api", "1")
                .appendQueryParameter("origin", origen[0] + "," + origen[1])
                .appendQueryParameter("destination", orden.get(orden.size() - 1).coordenadas());
        if (waypoints.length() > 0) {
            builder.appendQueryParameter("waypoints", waypoints.toString());
        }
        builder.appendQueryParameter("travelmode", modo);

        String url = builder.build().toString();
        Log.d(TAG, url);
        abrirGoogleMaps(Uri.parse(url));
    }

    // ---------------------------------------------------------------------
    // Invocar el servicio (Intent) de Google Maps
    // ---------------------------------------------------------------------
    private void abrirGoogleMaps(Uri gmmIntentUri) {
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage(MAPS_PACKAGE);
        try {
            startActivity(mapIntent);
        } catch (ActivityNotFoundException ex) {
            try {
                Intent unrestrictedIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                startActivity(unrestrictedIntent);
            } catch (ActivityNotFoundException innerEx) {
                Toast.makeText(this, "Please install a maps application", Toast.LENGTH_LONG).show();
            }
        }
    }

    // ---------------------------------------------------------------------
    // GPS del dispositivo
    // ---------------------------------------------------------------------
    @SuppressLint("MissingPermission")
    private void obtenerUbicacionGPS() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQ_UBICACION);
            return;
        }

        // Última ubicación conocida
        Location mejor = null;
        for (String proveedor : locationManager.getProviders(true)) {
            Location l = locationManager.getLastKnownLocation(proveedor);
            if (l != null && (mejor == null || l.getTime() > mejor.getTime())) mejor = l;
        }
        if (mejor != null) actualizarUbicacion(mejor);

        // Pedir una lectura nueva al GPS
        String proveedor;
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            proveedor = LocationManager.GPS_PROVIDER;
        } else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            proveedor = LocationManager.NETWORK_PROVIDER;
        } else {
            Toast.makeText(this, "Activa el GPS del dispositivo", Toast.LENGTH_LONG).show();
            return;
        }

        if (ubicacionActual == null) tvUbicacionActual.setText("Buscando señal GPS...");
        detenerGps();
        listenerGps = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                actualizarUbicacion(location);
                detenerGps();
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) { }

            @Override
            public void onProviderEnabled(@NonNull String provider) { }

            @Override
            public void onProviderDisabled(@NonNull String provider) { }
        };
        locationManager.requestLocationUpdates(proveedor, 0, 0, listenerGps, Looper.getMainLooper());
    }

    private void actualizarUbicacion(Location location) {
        ubicacionActual = location;
        double lat = location.getLatitude();
        double lng = location.getLongitude();
        // Si el emulador devuelve la ubicación por defecto de Google (Mountain View, CA), mostrar la Universidad
        if (Math.abs(lat - 37.422) < 0.1 && Math.abs(lng - (-122.084)) < 0.1) {
            tvUbicacionActual.setText("Mi ubicación (GPS): Universidad Piloto de Colombia");
        } else {
            tvUbicacionActual.setText(String.format(Locale.US, "Mi ubicación (GPS): %.6f, %.6f", lat, lng));
        }
    }

    private void detenerGps() {
        if (listenerGps != null) {
            locationManager.removeUpdates(listenerGps);
            listenerGps = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_UBICACION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                obtenerUbicacionGPS();
            } else {
                Toast.makeText(this, "Sin permiso de ubicación se usa la Universidad como punto de partida", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        detenerGps();
        super.onDestroy();
    }

    // ---------------------------------------------------------------------
    // Utilidades
    // ---------------------------------------------------------------------
    /** Ubicación GPS actual o, si no hay, la Universidad Piloto. */
    private double[] obtenerOrigen() {
        if (ubicacionActual != null) {
            double lat = ubicacionActual.getLatitude();
            double lng = ubicacionActual.getLongitude();
            // Si el emulador devuelve la ubicación por defecto de Google (Mountain View, CA), usar la Universidad
            if (Math.abs(lat - 37.422) < 0.1 && Math.abs(lng - (-122.084)) < 0.1) {
                return new double[]{UPC_LAT, UPC_LNG};
            }
            return new double[]{lat, lng};
        }
        Toast.makeText(this, "Sin ubicación GPS: se parte desde la Universidad Piloto", Toast.LENGTH_SHORT).show();
        return new double[]{UPC_LAT, UPC_LNG};
    }

    private String modoSeleccionado() {
        return MODOS_MAPS[spinnerModoTransporte.getSelectedItemPosition()];
    }

    private float distanciaMetros(double lat1, double lng1, double lat2, double lng2) {
        float[] resultado = new float[1];
        Location.distanceBetween(lat1, lng1, lat2, lng2, resultado);
        return resultado[0];
    }
}
