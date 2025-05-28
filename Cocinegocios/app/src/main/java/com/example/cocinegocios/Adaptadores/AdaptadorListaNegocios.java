package com.example.cocinegocios.Adaptadores;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.example.cocinegocios.Clases.Negocios;
import com.example.cocinegocios.Clases.Solicitudes;
import com.example.cocinegocios.Clases.Usuarios;
import com.example.cocinegocios.Clases.UsuariosSQLite;
import com.example.cocinegocios.MenuAdministrador;
import com.example.cocinegocios.R;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;

/**
 * Clase con funcion de adaptador para las listas que contengan negocios de la aplicación.
 * <p>
 * Esta clase o adaptador contiene la lógica necesaria para mostrar los negocios que haya en aplicación subidos a la base de datos.
 * <p>
 * Autor: [Juan Ramón de León Martín]
 * Fecha: [3/12/2024]
 */

public class AdaptadorListaNegocios extends ArrayAdapter<Negocios> {

    private ArrayList<Negocios> lista;
    private Context mContext;

    private TextView gmail;

    private String oficioUsuario, negocioUsuario;

    private String correoUsuario = "";

    private Boolean tieneOficio = false;

    private StorageReference mStorage;

    private Bundle negocioSeleccionado;

    SQLiteDatabase baseDatos;


    /**
     * Constructor del adaptador de la lista de negocios.
     * <p>
     * Este constructor inicializa el adaptador con el negocios, la lista de negocios.
     *
     * @param contexto El contexto de la actividad o fragmento donde se utiliza el adaptador.
     * @param lista La lista de objetos de tipo Negocios que se mostrarán en la vista.
     */
    public AdaptadorListaNegocios(Context contexto, ArrayList<Negocios> lista) {
        super(contexto, R.layout.elemento_negocios, lista);
        this.lista = lista;
        this.mContext = contexto;
        this.mStorage = FirebaseStorage.getInstance().getReference();
    }

    /**
     * Metodo para inicializar el contenido del listado de negocios.
     * <p>
     * Esta clase inicializa el contenido de cada uno de los negocios de la lista de negocios.
     */
    public static class ViewHolder {
        TextView nombre;
        TextView gmail;
        TextView localidad;
        ImageView imagen;
        Button verMas;
    }

    /**
     * Metodo para dar utilidad a los negocios
     * <p>
     * Esta clase coge y asocia los contenidos de cada elemento negocios y los asocia con los del 'elemento_negocios' mediante los ViewHolder. El ViewHolder se crea si no hay uno previamente,en el
     * caso contrario se reutiliza. Luego se pone los datos correspondientes a los TextView e ImageView y se crea la logica del botón visualizar, donde el usuario podra acceder como administrado,
     * mandar solicitudes o acceder como camarero o cocinero.
     *
     * @param position La posición del elemento en la lista que se está procesando.
     * @param convertView La vista del elemento negocio en el que se trabaja.
     * @param parent Los datos usados para conocer las dimensiones y las propiedades del contenedor.
     *
     * @return convertView La vista convertida en viewwHolder para mostrar el negocio.
     */
    @SuppressLint("Range")
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        ViewHolder viewHolder;

        negocioSeleccionado = new Bundle();

        //Reutilizar la vista existente si es posible
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            convertView = inflater.inflate(R.layout.elemento_negocios, parent, false);

            //Crear un nuevo ViewHolder
            viewHolder = new ViewHolder();
            viewHolder.nombre = convertView.findViewById(R.id.nombreNegocioList);
            viewHolder.gmail = convertView.findViewById(R.id.gmailNegocioList);
            viewHolder.localidad = convertView.findViewById(R.id.localidadNegocioList);
            viewHolder.imagen = convertView.findViewById(R.id.imagenNegocioList);
            viewHolder.verMas = convertView.findViewById(R.id.botonMasInformacionNegocio);

            convertView.setTag(viewHolder);
        } else {
            //Si la vista ya existe, reutilizar el ViewHolder
            viewHolder = (ViewHolder) convertView.getTag();
        }

        //Buscamos el rol del usuario actual, para en el caso de necesitar gestionar algo dependiendo de su rol, poder hacerlo
        UsuariosSQLite baseDatosUsuarios = new UsuariosSQLite(mContext, "bbddUsuarios", null, 1);
        baseDatos = baseDatosUsuarios.getWritableDatabase();

        String consultaUsuario = "SELECT correo, contrasena FROM usuario LIMIT 1";
        Cursor cursor = baseDatos.rawQuery(consultaUsuario, null);

        if (cursor.moveToFirst()) {
            correoUsuario = cursor.getString(cursor.getColumnIndex("correo"));
        }
        cursor.close();

        //Obtener los datos del negocio actual
        Negocios negocioActual = lista.get(position);

        //Asignar datos a las vistas
        viewHolder.nombre.setText(negocioActual.getNombre());
        String gmailDescodificado = negocioActual.getGmail().replace("_", ".");
        viewHolder.gmail.setText(gmailDescodificado);
        viewHolder.localidad.setText(negocioActual.getLocalidad());

        //Obtener la imagen desde Firebase Storage
        String enlaceImagen = negocioActual.getImagenCodificada();
        String direcciónReal = enlaceImagen.replace("gs://negocios-de-cocinas.appspot.com/", "");
        StorageReference imageRef = mStorage.child(direcciónReal);

        //Limpiar la imagen anterior mientras se carga la nueva
        viewHolder.imagen.setImageDrawable(null);

        //Cargar la imagen usando Glide
        imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
            Glide.with(mContext)
                    .load(uri)
                    .into(viewHolder.imagen);
        }).addOnFailureListener(exception -> {
            Log.e("STORAGE", "Error al obtener la URL de la imagen", exception);
        });

        //Logica para mirar si el usuario actual tiene oficio y en que negocio, para mas posteriormente entrar como camarero, cocinero o administrador del negocio.
        DatabaseReference databaseReferenceCreacion = FirebaseDatabase.getInstance("https://negocios-de-cocinas-default-rtdb.europe-west1.firebasedatabase.app/").getReference();
        DatabaseReference usuarioReferencia = databaseReferenceCreacion.child("Usuarios");

        usuarioReferencia.child(correoUsuario).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Usuarios usuarioEncontrado = dataSnapshot.getValue(Usuarios.class);
                    if (usuarioEncontrado != null) {
                        oficioUsuario = usuarioEncontrado.getOficio();
                        negocioUsuario = usuarioEncontrado.getNegocioOficio();
                    }
                } else {
                    Snackbar.make(parent, "El correo '"+gmail + "' no existe en la base de datos", Snackbar.LENGTH_SHORT).show();
                }

                if (oficioUsuario != null && (oficioUsuario.equals("Cocinero") || oficioUsuario.equals("Camarero"))) {
                    tieneOficio = true;
                } else {
                    tieneOficio = false;
                }

            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Snackbar.make(parent, "Error de base de datos", Snackbar.LENGTH_SHORT).show();
            }
        });

        /*
         * Se da lógica al boton que sirve para redirigir al usuario como administrador, cocinero o camarero, si no se manda una solicitud para acceder al mismo de las 2 últimas formas
         */
        viewHolder.verMas.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Cargar el dialogo ver más para tener las opciones del usuario
                Dialog dialogoOpcionesNegocio = new Dialog(mContext);
                dialogoOpcionesNegocio.setContentView(R.layout.dialogo_ver_mas);
                dialogoOpcionesNegocio.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

                Button botonAdmin = dialogoOpcionesNegocio.findViewById(R.id.botonAccederAdmin);
                Button botonSolicitar = dialogoOpcionesNegocio.findViewById(R.id.botonSolicitarAcceso);

                //En el caso de que el usuario ya tenga rol de camarero o cocinero cambiar el boton de solicitud por acceder y tener otra funcion que sea la de acceder al negocio siendo cocinero o camarero
                if (tieneOficio && negocioUsuario.equals(negocioActual.getGmail())) {
                    botonSolicitar.setText("Acceder al local");
                }

                String correoNegocio = negocioActual.getGmail();

                /*
                 * Se da lógica al boton que sirve para redirigir al usuario como administrador y poner como rol en SQLite administrador para poder gestionar sus opciones
                 */
                botonAdmin.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        // Comparar los correos
                        if (correoUsuario.equals(correoNegocio)) {

                            String actualizacionRolUsuario = "UPDATE usuario SET rol = ? WHERE correo = ?";
                            SQLiteStatement statement = baseDatos.compileStatement(actualizacionRolUsuario);
                            statement.bindString(1, "Administrador");
                            statement.bindString(2, correoUsuario);
                            statement.executeUpdateDelete();

                            // Los correos coinciden, permite el acceso a la pantalla de administrador
                            negocioSeleccionado.putString("negocioLoggued", correoNegocio);

                            Intent accederAdministrador = new Intent(mContext, MenuAdministrador.class);
                            accederAdministrador.putExtras(negocioSeleccionado);
                            mContext.startActivity(accederAdministrador);
                        } else {
                            Snackbar.make(v, "Acceso denegado: No eres su administrador.", Snackbar.LENGTH_SHORT).show();
                        }
                    }
                });

                /*
                 * Se da lógica al boton que sirve para solicitar participar en el negocio como cocinero o camarero
                 */
                botonSolicitar.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //Se comprueba si tiene oficio el usuario y es en el negocio actual, en el caso de ser asi, acceder y otorgar rol en SQLite del rol en Firebase
                        if (tieneOficio && negocioUsuario.equals(negocioActual.getGmail())) {
                            String actualizacionRolUsuario = "UPDATE usuario SET rol = ? WHERE correo = ?";
                            SQLiteStatement statement = baseDatos.compileStatement(actualizacionRolUsuario);
                            if (oficioUsuario.equals("Cocinero")) {
                                statement.bindString(1, "Cocinero");
                            } else {
                                statement.bindString(1, "Camarero");
                            }
                            statement.bindString(2, correoUsuario);
                            statement.executeUpdateDelete();

                            negocioSeleccionado.putString("negocioLoggued", correoNegocio);

                            Intent accederAdministrador = new Intent(mContext, MenuAdministrador.class);
                            accederAdministrador.putExtras(negocioSeleccionado);
                            mContext.startActivity(accederAdministrador);

                            //En el caso de no tener ningun rol y no trabajar en ningun negocio, se manda una solicitud con los datos del usuario actual al negocio elegido
                        } else {
                            String correoNegocio = negocioActual.getGmail();
                            String id = correoNegocio+"*"+correoUsuario;

                            DatabaseReference referenciaUsuarios = FirebaseDatabase.getInstance().getReference("Usuarios");
                            Query busquedaUsuarios = referenciaUsuarios.orderByChild("gmail").equalTo(correoUsuario);

                            busquedaUsuarios.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    if (dataSnapshot.exists()) {
                                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                            Usuarios usuarioEncontrado = snapshot.getValue(Usuarios.class);
                                            if (usuarioEncontrado != null) {
                                                //Si existe el usuario, crear una solicitud con sus datos
                                                DatabaseReference databaseReferenceCreacion = FirebaseDatabase.getInstance("https://negocios-de-cocinas-default-rtdb.europe-west1.firebasedatabase.app/").getReference();
                                                DatabaseReference solicitudReferencia = databaseReferenceCreacion.child("Solicitudes");

                                                String nombre = usuarioEncontrado.getNombre();
                                                String apellidos = usuarioEncontrado.getApellidos();
                                                String fecha = usuarioEncontrado.getFecha();
                                                String DNI = usuarioEncontrado.getDNI();

                                                solicitudReferencia.child(id).addListenerForSingleValueEvent(new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                        if (snapshot.exists()) {
                                                            Snackbar.make(v, "La solicitud a este local ya fue enviada previamente", Snackbar.LENGTH_SHORT).show();
                                                        } else {
                                                            Solicitudes solicitudNueva = new Solicitudes(id, correoNegocio, correoUsuario, nombre, apellidos, fecha, DNI);
                                                            solicitudReferencia.child(id).setValue(solicitudNueva).addOnSuccessListener(aVoid -> {
                                                                        Snackbar.make(v, "Solicitud enviada correctamente", Snackbar.LENGTH_SHORT).show();
                                                                    })
                                                                    .addOnFailureListener(e -> {
                                                                        Snackbar.make(v, "Usuario sin registrar", Snackbar.LENGTH_SHORT).show();
                                                                    });
                                                        }
                                                    }

                                                    @Override
                                                    public void onCancelled(@NonNull DatabaseError error) {
                                                        Snackbar.make(v, "Error de conexión con la base de datos", Snackbar.LENGTH_SHORT).show();
                                                    }
                                                });
                                            }
                                        }
                                    } else {
                                        Snackbar.make(v, "El usuario '"+correoUsuario + "' no existe en la base de datos", Snackbar.LENGTH_SHORT).show();
                                    }
                                }
                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {
                                    Snackbar.make(v, "Error de base de datos", Snackbar.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }
                });

                dialogoOpcionesNegocio.show();
            }
        });

        return convertView;
    }
}
