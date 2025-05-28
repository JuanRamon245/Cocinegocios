package com.example.cocinegocios.Adaptadores;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.cocinegocios.Clases.Solicitudes;
import com.example.cocinegocios.Clases.Usuarios;
import com.example.cocinegocios.Fragmentos.TrabajadoresDelNegocio;
import com.example.cocinegocios.GestionarTrabajadores;
import com.example.cocinegocios.R;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;

/**
 * Clase con funcion de adaptador para las listas que contengan solicitudes del negocio.
 * <p>
 * Esta clase o adaptador contiene la lógica necesaria para mostrar las solicitudes que haya en el negocio donde él usuario esté actualmente.
 * <p>
 * Autor: [Juan Ramón de León Martín]
 * Fecha: [3/12/2024]
 */

public class AdaptadorListaSolicitudes extends ArrayAdapter<Solicitudes> {

    private ArrayList<Solicitudes> lista;
    private Context mContext;

    private StorageReference mStorage;

    /**
     * Constructor del adaptador de la lista de productos.
     * <p>
     * Este constructor inicializa el adaptador con el contexto y la lista de solicitudes del negocio en el que estamos actualmente.
     *
     * @param contexto El contexto de la actividad o fragmento donde se utiliza el adaptador.
     * @param lista La lista de objetos de tipo Solicityd que se mostrarán en la vista.
     */
    public AdaptadorListaSolicitudes(Context contexto, ArrayList<Solicitudes> lista) {
        super(contexto, R.layout.elemento_solicitud, lista);
        this.lista = lista;
        this.mContext = contexto;
        this.mStorage = FirebaseStorage.getInstance().getReference();
    }

    /**
     * Metodo para inicializar el contenido del listado de solicitudes.
     * <p>
     * Esta clase inicializa el contenido de cada uno de las solicitudes de la lista de solicitudes.
     */
    public static class ViewHolder {
        TextView nombreUsuario;
        TextView apellidosUsuario;
        TextView DNIUsuario;
        TextView fechaNacimiento;
        Button botonRechazarTrabajador;
        Button botonAceptarTrabajador;
    }

    /**
     * Metodo para dar utilidad a las solicitudes
     * <p>
     * Esta clase coge y asocia los contenidos de cada elemento solicitud y los asocia con los del 'elemento_solicitud' mediante los ViewHolder. El ViewHolder se crea si no hay uno previamente,en el
     * caso contrario se reutiliza. Luego se pone los datos correspondientes a los TextView y se crea la logica del botón rechazar trabajador en el caso de no quererlo añadir, pero en el caso contrario
     * el boton de añadir trabajador permite hacer ese usuario parte de la plantilla del negocio
     *
     * @param position La posición del elemento en la lista que se está procesando.
     * @param convertView La vista del elemento solicitud en el que se trabaja.
     * @param parent Los datos usados para conocer las dimensiones y las propiedades del contenedor.
     *
     * @return convertView La vista convertida en viewwHolder para mostrar el producto.
     */
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        AdaptadorListaSolicitudes.ViewHolder viewHolder;

        //Reutilizar la vista existente si es posible
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            convertView = inflater.inflate(R.layout.elemento_solicitud, parent, false);

            //Crear un nuevo ViewHolder
            viewHolder = new AdaptadorListaSolicitudes.ViewHolder();
            viewHolder.nombreUsuario = convertView.findViewById(R.id.nombreUsuario);
            viewHolder.apellidosUsuario = convertView.findViewById(R.id.apellidosUsuario);
            viewHolder.DNIUsuario = convertView.findViewById(R.id.DNIUsuario);
            viewHolder.fechaNacimiento = convertView.findViewById(R.id.fechaNacimiento);
            viewHolder.botonRechazarTrabajador = convertView.findViewById(R.id.botonRechazarTrabajador);
            viewHolder.botonAceptarTrabajador = convertView.findViewById(R.id.botonAceptarTrabajador);

            convertView.setTag(viewHolder);
        } else {
            //Si la vista ya existe, reutilizar el ViewHolder
            viewHolder = (AdaptadorListaSolicitudes.ViewHolder) convertView.getTag();
        }

        //Obtener los datos del solicitud actual
        Solicitudes solicitudActual = lista.get(position);

        viewHolder.nombreUsuario.setText(solicitudActual.getNombre());
        viewHolder.apellidosUsuario.setText(solicitudActual.getApellidos());
        viewHolder.DNIUsuario.setText(solicitudActual.getDNI());
        viewHolder.fechaNacimiento.setText(solicitudActual.getFecha());

        DatabaseReference databaseReferenceCreacion = FirebaseDatabase.getInstance("https://negocios-de-cocinas-default-rtdb.europe-west1.firebasedatabase.app/").getReference();
        DatabaseReference usuarioReferencia = databaseReferenceCreacion.child("Usuarios");
        DatabaseReference solicitudReferencia = databaseReferenceCreacion.child("Solicitudes").child(solicitudActual.getId());

        View finalConvertView = convertView;

        /*
         * Se da lógica al boton que sirve para aceptar al trabajador dentro del negocio y que pueda ponerse a trabajar
         */
        viewHolder.botonAceptarTrabajador.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Dialog dialogoElegirRol = new Dialog(mContext);
                dialogoElegirRol.setContentView(R.layout.dialogo_elegir_rol);
                dialogoElegirRol.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)); // Fondo transparente para evitar bordes

                RadioGroup rolGroup = dialogoElegirRol.findViewById(R.id.rolUsuarioGroup);
                RadioButton rolCamarero = dialogoElegirRol.findViewById(R.id.rolCamarero);
                RadioButton rolCocinero = dialogoElegirRol.findViewById(R.id.rolCocinero);
                rolCamarero.setChecked(true);

                Button botonAceptarUsuarioEmpresa = dialogoElegirRol.findViewById(R.id.botonAceptarUsuarioEmpresa);
                botonAceptarUsuarioEmpresa.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String idUsuario = solicitudActual.getCorreoUsuario();

                        usuarioReferencia.child(idUsuario).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if (dataSnapshot.exists()) {
                                    Usuarios usuarioEncontrado = dataSnapshot.getValue(Usuarios.class);
                                    //Log.d("PRUEBA", "2");
                                    usuarioEncontrado.setNegocioOficio(solicitudActual.getCorreoNegocio());
                                    Log.d("PRUEBA", "4");

                                    int selectedId = rolGroup.getCheckedRadioButtonId();
                                    String oficioSeleccionado;
                                    if (selectedId == R.id.rolCamarero) {
                                        oficioSeleccionado = "Camarero";
                                    } else if (selectedId == R.id.rolCocinero) {
                                        oficioSeleccionado = "Cocinero";
                                    } else {
                                        oficioSeleccionado = ""; // Si no se seleccionó nada, por seguridad
                                    }

                                    usuarioEncontrado.setOficio(oficioSeleccionado);

                                    usuarioReferencia.child(idUsuario).setValue(usuarioEncontrado).addOnSuccessListener(aVoid -> {
                                        solicitudReferencia.removeValue().addOnSuccessListener(aVoid1 -> {
                                            // Actualizar la lista local
                                            lista.remove(solicitudActual);
                                            notifyDataSetChanged(); // Notificar al adaptador de los cambios
                                            Snackbar.make(v, "Usuario aceptado y solicitud eliminada", Snackbar.LENGTH_SHORT).show();

                                            if (mContext instanceof GestionarTrabajadores) {
                                                GestionarTrabajadores actividadPrincipal = (GestionarTrabajadores) mContext;
                                                TrabajadoresDelNegocio fragmento = (TrabajadoresDelNegocio) actividadPrincipal.getSupportFragmentManager()
                                                        .findFragmentByTag("TrabajadoresDelNegocio");
                                                if (fragmento != null) {
                                                    fragmento.actualizarListaUsuariosTrabajadores();
                                                }
                                            }

                                            dialogoElegirRol.dismiss(); // Cerrar el diálogo
                                        }).addOnFailureListener(e -> {
                                            Snackbar.make(v, "Error al eliminar la solicitud", Snackbar.LENGTH_SHORT).show();
                                        });
                                    }).addOnFailureListener(e -> {
                                        Snackbar.make(v, "Error al actualizar el usuario", Snackbar.LENGTH_SHORT).show();
                                    });
                                } else {
                                    Snackbar.make(v, "El usuario '"+solicitudActual.getNombre() + "' no existe en la base de datos", Snackbar.LENGTH_SHORT).show();
                                }
                            }
                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                Snackbar.make(v, "Error de base de datos", Snackbar.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
                dialogoElegirRol.show();
            }
        });

        /*
         * Se da lógica al boton que sirve para aceptar al trabajador dentro del negocio y que pueda ponerse a trabajar
         */
        viewHolder.botonRechazarTrabajador.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                solicitudReferencia.removeValue().addOnSuccessListener(aVoid1 -> {
                    // Actualizar la lista local
                    lista.remove(solicitudActual);
                    notifyDataSetChanged(); // Notificar al adaptador de los cambios
                    Snackbar.make(v, "Usuario aceptado y solicitud eliminada", Snackbar.LENGTH_SHORT).show();
                }).addOnFailureListener(e -> {
                    Snackbar.make(v, "Error al eliminar la solicitud", Snackbar.LENGTH_SHORT).show();
                });
            }
        });

        return convertView;
    }
}
