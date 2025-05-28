package com.example.cocinegocios.Adaptadores;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
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
import androidx.annotation.RequiresApi;

import com.bumptech.glide.Glide;
import com.example.cocinegocios.Clases.Usuarios;
import com.example.cocinegocios.R;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;

/**
 * Clase con funcion de adaptador para las listas que contengan trabajadores del negocio.
 * <p>
 * Esta clase o adaptador contiene la lógica necesaria para mostrar los trabajadores que haya en el negocio donde él usuario esté actualmente.
 * <p>
 * Autor: [Juan Ramón de León Martín]
 * Fecha: [3/12/2024]
 */

public class AdaptadorListaTrabajadores extends ArrayAdapter<Usuarios> {

    private ArrayList<Usuarios> lista;
    private Context mContext;

    private StorageReference mStorage;

    /**
     * Constructor del adaptador de la lista de trabajadores.
     * <p>
     * Este constructor inicializa el adaptador con el contexto y la lista de trabajadores del negocio en el que estamos actualmente.
     *
     * @param contexto El contexto de la actividad o fragmento donde se utiliza el adaptador.
     * @param lista La lista de objetos de tipo Usuario que se mostrarán en la vista.
     */
    public AdaptadorListaTrabajadores(Context contexto, ArrayList<Usuarios> lista) {
        super(contexto, R.layout.elemento_usuario_aceptado, lista);
        this.lista = lista;
        this.mContext = contexto;
        this.mStorage = FirebaseStorage.getInstance().getReference();
    }

    /**
     * Metodo para inicializar el contenido del listado de solicitudes.
     * <p>
     * Esta clase inicializa el contenido de cada uno de los trabajadores de la lista de trabajadores.
     */
    public static class ViewHolder {
        TextView nombreUsuario;
        TextView apellidosUsuario;
        TextView oficioUsuario;
        TextView edadUsuario;
        Button botonMasInformacionUsuario;
    }


    /**
     * Metodo para dar utilidad a las solicitudes
     * <p>
     * Esta clase coge y asocia los contenidos de cada elemento ususario y los asocia con los del 'elemento_usuario_aceptado' mediante los ViewHolder. El ViewHolder se crea si no hay uno previamente,en el
     * caso contrario se reutiliza. Luego se pone los datos correspondientes a los TextView y se crea la logica del botón obtener más información para asi poder egstionar máss apropiadamente al usuario con
     * sus datos incluidos, siendo estas posibilidades la de eliminar el trabajador o cambiar de rol por medio de su dialogo correspondiente.
     *
     * @param position La posición del elemento en la lista que se está procesando.
     * @param convertView La vista del elemento usuario en el que se trabaja.
     * @param parent Los datos usados para conocer las dimensiones y las propiedades del contenedor.
     *
     * @return convertView La vista convertida en viewwHolder para mostrar el producto.
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        AdaptadorListaTrabajadores.ViewHolder viewHolder;

        //Reutilizar la vista existente si es posible
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            convertView = inflater.inflate(R.layout.elemento_usuario_aceptado, parent, false);

            // Crear un nuevo ViewHolder
            viewHolder = new AdaptadorListaTrabajadores.ViewHolder();
            viewHolder.nombreUsuario = convertView.findViewById(R.id.nombreUsuario);
            viewHolder.apellidosUsuario = convertView.findViewById(R.id.apellidosUsuario);
            viewHolder.oficioUsuario = convertView.findViewById(R.id.oficioUsuario);
            viewHolder.edadUsuario = convertView.findViewById(R.id.edadUsuario);
            viewHolder.botonMasInformacionUsuario = convertView.findViewById(R.id.botonMasInformacionUsuario);

            convertView.setTag(viewHolder);
        } else {
            // Si la vista ya existe, reutilizar el ViewHolder
            viewHolder = (AdaptadorListaTrabajadores.ViewHolder) convertView.getTag();
        }

        //Obtener los datos del usuario actual
        Usuarios usuarioActual = lista.get(position);

        viewHolder.nombreUsuario.setText(usuarioActual.getNombre());
        viewHolder.apellidosUsuario.setText(usuarioActual.getApellidos());
        viewHolder.oficioUsuario.setText(usuarioActual.getOficio());
        String fechaNacimiento = usuarioActual.getFecha();
        int edad = calcularEdad(fechaNacimiento);
        viewHolder.edadUsuario.setText(String.valueOf(edad));

        /*
         * Se da lógica al boton que sirve para mostrar el dialogo del usuario con toda su informacion, ademas de poder gestionarlo
         */
        viewHolder.botonMasInformacionUsuario.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Dialog dialogoInformacionUsuarios = new Dialog(mContext);
                dialogoInformacionUsuarios.setContentView(R.layout.dialogo_informacion_usuario);
                dialogoInformacionUsuarios.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)); // Fondo transparente para evitar bordes

                ImageView fotoPerfil = dialogoInformacionUsuarios.findViewById(R.id.imagenLocalRegistro);

                //Obtener la imagen desde Firebase Storage
                String enlaceImagen = usuarioActual.getFotoPerfil();
                String direcciónReal = enlaceImagen.replace("gs://negocios-de-cocinas.appspot.com/", "");
                StorageReference imageRef = mStorage.child(direcciónReal);

                //Limpiar la imagen anterior mientras se carga la nueva
                fotoPerfil.setImageDrawable(null);

                //Cargar la imagen usando Glide
                imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    Glide.with(mContext)
                            .load(uri)
                            .into(fotoPerfil);
                }).addOnFailureListener(exception -> {
                    Log.e("STORAGE", "Error al obtener la URL de la imagen", exception);
                });

                TextView nombreUsuario = dialogoInformacionUsuarios.findViewById(R.id.nombreUsuario);
                nombreUsuario.setText(usuarioActual.getNombre());

                TextView apellidosUsuario = dialogoInformacionUsuarios.findViewById(R.id.apellidosUsuario);
                apellidosUsuario.setText(usuarioActual.getApellidos());

                TextView correoUsuario = dialogoInformacionUsuarios.findViewById(R.id.correoUsuario);
                correoUsuario.setText(usuarioActual.getGmail().replace("_", "."));

                TextView DNIUsuario = dialogoInformacionUsuarios.findViewById(R.id.DNIUsuario);
                DNIUsuario.setText(usuarioActual.getDNI());

                TextView fechaNacimiento = dialogoInformacionUsuarios.findViewById(R.id.fechaNacimiento);
                fechaNacimiento.setText(usuarioActual.getFecha());

                TextView telefonoUsuario = dialogoInformacionUsuarios.findViewById(R.id.telefonoUsuario);
                telefonoUsuario.setText(String.valueOf(usuarioActual.getTelefono()));

                TextView oficioUsuario = dialogoInformacionUsuarios.findViewById(R.id.oficioUsuario);
                oficioUsuario.setText(usuarioActual.getOficio());

                Button botonBorrar = dialogoInformacionUsuarios.findViewById(R.id.botonEliminarTrabajador);
                Button botonCambiarOficio = dialogoInformacionUsuarios.findViewById(R.id.botonCambiarOficioTrabajador);

                DatabaseReference databaseReferenceCreacion = FirebaseDatabase.getInstance("https://negocios-de-cocinas-default-rtdb.europe-west1.firebasedatabase.app/").getReference();
                DatabaseReference usuarioReferencia = databaseReferenceCreacion.child("Usuarios");

                /*
                 * Se da lógica al boton que sirve para despedir o expulsar al trabajador del negocio donde nos encontramos actualmente.
                 */
                botonBorrar.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String idUsuario = usuarioActual.getGmail();

                        //Se busca si el usuario seleccionado existe y en vez de elimnarlo como tal, lo que hacemos es editar sus parametros de negocioOficio y oficio, para que simule que no tiene
                        usuarioReferencia.child(idUsuario).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if (dataSnapshot.exists()) {
                                    Usuarios usuarioEncontrado = dataSnapshot.getValue(Usuarios.class);
                                    usuarioEncontrado.setNegocioOficio("sin");
                                    usuarioEncontrado.setOficio("sin");

                                    usuarioReferencia.child(idUsuario).setValue(usuarioEncontrado).addOnSuccessListener(aVoid -> {
                                        Snackbar.make(v, "Usuario actualizado eliminada", Snackbar.LENGTH_SHORT).show();
                                    }).addOnFailureListener(e -> {
                                        Snackbar.make(v, "Error al actualizar el usuario", Snackbar.LENGTH_SHORT).show();
                                    });

                                    notifyDataSetChanged();
                                    dialogoInformacionUsuarios.dismiss();
                                } else {
                                    Snackbar.make(v, "El usuario '"+usuarioActual.getNombre() + "' no existe en la base de datos", Snackbar.LENGTH_SHORT).show();
                                }
                            }
                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                Snackbar.make(v, "Error de base de datos", Snackbar.LENGTH_SHORT).show();
                            }
                        });
                    }
                });

                /*
                 * Se da lógica al boton que sirve para cambiar de oficio al usuario elegido, en este caso como solo hay 2 oficios, si el usuario está en uno, cambia al otro automaticamente
                 */
                botonCambiarOficio.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String idUsuario = usuarioActual.getGmail();

                        usuarioReferencia.child(idUsuario).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if (dataSnapshot.exists()) {
                                    Usuarios usuarioEncontrado = dataSnapshot.getValue(Usuarios.class);
                                    String oficioActual = usuarioEncontrado.getOficio();
                                    if (oficioActual.equals("Cocinero")) {
                                        usuarioEncontrado.setOficio("Camarero");
                                    } else {
                                        usuarioEncontrado.setOficio("Cocinero");
                                    }
                                    usuarioReferencia.child(idUsuario).setValue(usuarioEncontrado).addOnSuccessListener(aVoid -> {
                                        Snackbar.make(v, "Usuario actualizado eliminada", Snackbar.LENGTH_SHORT).show();
                                    }).addOnFailureListener(e -> {
                                        Snackbar.make(v, "Error al actualizar el usuario", Snackbar.LENGTH_SHORT).show();
                                    });

                                    notifyDataSetChanged();
                                    dialogoInformacionUsuarios.dismiss();
                                } else {
                                    Snackbar.make(v, "El usuario '"+usuarioActual.getNombre() + "' no existe en la base de datos", Snackbar.LENGTH_SHORT).show();
                                }
                            }
                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                Snackbar.make(v, "Error de base de datos", Snackbar.LENGTH_SHORT).show();
                            }
                        });
                    }
                });


                dialogoInformacionUsuarios.show();
            }
        });

        return convertView;
    }

    /**
     * Método que sirve para calcular la edad a tiempo real del usuario, restando su fecha de nacimiento con la fecha del dispositivo. Este programa necesita una api en adelante especifica para hacer
     * este funcionamiento, por eso getView contiene ese '@RequiresApi(api = Build.VERSION_CODES.O)'
     *
     * @param fechaNacimiento fecha de nacimiento del trabajador
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public int calcularEdad(String fechaNacimiento) {
        try {
            //Formato de la fecha de nacimiento
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            //Convertir la fecha de nacimiento en un LocalDate
            LocalDate fechaNac = LocalDate.parse(fechaNacimiento, formatter);
            //Obtener la fecha actual del dispositivo
            LocalDate fechaActual = LocalDate.now();

            return Period.between(fechaNac, fechaActual).getYears();
        } catch (DateTimeParseException e) {
            e.printStackTrace();
            return -1;
        }
    }
}
