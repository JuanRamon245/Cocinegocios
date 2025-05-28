package com.example.cocinegocios.Adaptadores;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.cocinegocios.Clases.Espacio;
import com.example.cocinegocios.R;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

/**
 * Clase con funcion de adaptador para las listas que contengan espacios del negocio.
 * <p>
 * Esta clase o adaptador contiene la lógica necesaria para mostrar los espacios que haya en el negocio donde él usuario esté actualmente.
 * <p>
 * Autor: [Juan Ramón de León Martín]
 * Fecha: [3/12/2024]
 */

public class AdaptadorListaEspacios extends ArrayAdapter<Espacio> {

    private ArrayList<Espacio> lista;
    private Context mContext;

    private String negocioActual;

    /**
     * Constructor del adaptador de la lista de espacios.
     * <p>
     * Este constructor inicializa el adaptador con el contexto, la lista de espacios, y el nombre del negocio en el que estamos actualmente.
     *
     * @param contexto El contexto de la actividad o fragmento donde se utiliza el adaptador.
     * @param lista La lista de objetos de tipo Espacio que se mostrarán en la vista.
     * @param nombreNegocio El nombre del negocio en el que estamos actualmente.
     */
    public AdaptadorListaEspacios(Context contexto, ArrayList<Espacio> lista, String nombreNegocio) {
        super(contexto, R.layout.elemento_espacio, lista);
        this.lista = lista;
        this.mContext = contexto;
        this.negocioActual = nombreNegocio;

    }

    /**
     * Metodo para inicializar el contenido del listado de espacios.
     * <p>
     * Esta clase inicializa el contenido de cada uno de los espacios de la lista de espacios.
     */
    public static class ViewHolder {
        TextView nombreEspacio;
        TextView numeroMesas;
        Button editar;
        Button eliminar;
    }

    /**
     * Metodo para dar utilidad a los espacios
     * <p>
     * Esta clase coge y asocia los contenidos de cada elemento espacio y los asocia con los del 'elemento_espacio' mediante los ViewHolder. El ViewHolder se crea si no hay uno previamente,en el
     * caso contrario se reutiliza. Luego se pone los datos correspondientes a los TextView y se crea la logica del botón eliminar, que por su puesto elimina el espacio del negocio asociado,pero el
     * botón editar sirve para aabrir un dialogo para poder cambiar el numero de mesas del espacio elegido, porque el nombre es usado para generar el ID del espacio y por eso no se puede cambiar.
     *
     * @param position La posición del elemento en la lista que se está procesando.
     * @param convertView La vista del elemento espacio en el que se trabaja.
     * @param parent Los datos usados para conocer las dimensiones y las propiedades del contenedor.
     *
     * @return convertView La vista convertida en viewwHolder para mostrar el espacio.
     */
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        AdaptadorListaEspacios.ViewHolder viewHolder;

        //Reutilizar la vista existente si es posible
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            convertView = inflater.inflate(R.layout.elemento_espacio, parent, false);

            //Crear un nuevo ViewHolder
            viewHolder = new AdaptadorListaEspacios.ViewHolder();
            viewHolder.nombreEspacio = convertView.findViewById(R.id.nombreEspacio);
            viewHolder.numeroMesas = convertView.findViewById(R.id.numeroMesas);
            viewHolder.editar = convertView.findViewById(R.id.botonEditarEspacio);
            viewHolder.eliminar = convertView.findViewById(R.id.botonEliminarEspacio);

            convertView.setTag(viewHolder);
        } else {
            //Si la vista ya existe, reutilizar el ViewHolder
            viewHolder = (AdaptadorListaEspacios.ViewHolder) convertView.getTag();
        }

        //Obtener los datos del negocio actual
        Espacio espacioActual = lista.get(position);

        viewHolder.nombreEspacio.setText(espacioActual.getNombre());
        viewHolder.numeroMesas.setText(String.valueOf(espacioActual.getnMesas()));

        /*
         * Se da lógica al boton que sirve para eliminar el espacio, mediante la busqueda por el ID del espacio seleccionado, y tras esto se elimina.
         */
        viewHolder.eliminar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String claveEspacio = espacioActual.getId();
                DatabaseReference databaseReferenceProductos = FirebaseDatabase.getInstance().getReference("espacio");
                Query busquedaEspacios = databaseReferenceProductos.orderByChild("id").equalTo(claveEspacio);

                busquedaEspacios.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                snapshot.getRef().removeValue();
                            }
                            lista.remove(position);
                            notifyDataSetChanged();
                        } else {
                            Snackbar.make(v, "El espacio '"+espacioActual.getNombre() + "' no existe en la base de datos", Snackbar.LENGTH_SHORT).show();
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
         * Se da lógica al boton que sirve para editar el espacio, mediante la busqueda por el ID del espacio seleccionado, y tras esto se abre un dialogo para poder cambiar
         * el número de mesas del espacio
         */
        viewHolder.editar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Dialog dialogoEspacios = new Dialog(mContext);
                dialogoEspacios.setContentView(R.layout.dialogo_editar_espacio);
                dialogoEspacios.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

                EditText textoNombre = dialogoEspacios.findViewById(R.id.nombrePropuestoEspacio);
                textoNombre.setText(espacioActual.getNombre());
                textoNombre.setEnabled(false);

                EditText textoNMesas = dialogoEspacios.findViewById(R.id.nMesasPropuestoEspacio);
                textoNMesas.setText(String.valueOf(espacioActual.getnMesas()));

                /*
                 * Se da lógica al boton que sirve para editar el espacio dentro del dialogo, cogiendo el id anterior, y cambiandole los atributos por los nuevos
                 */
                Button botonEditar = dialogoEspacios.findViewById(R.id.botonEditarEspacio);
                botonEditar.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String claveEspacio = espacioActual.getId();
                        DatabaseReference referenciaEspacio = FirebaseDatabase.getInstance().getReference("espacio");
                        Query busquedaEspacio = referenciaEspacio.orderByChild("id").equalTo(claveEspacio);

                        //Convertimos el numero de mesas de un String a un Int
                        String nuevoNMesasTexto = textoNMesas.getText().toString().trim();
                        if (nuevoNMesasTexto.isEmpty()) {
                            Snackbar.make(v, "El número de mesas no puede estar vacío", Snackbar.LENGTH_SHORT).show();
                            return;
                        }

                        int nuevoNMesas;
                        try {
                            nuevoNMesas = Integer.parseInt(nuevoNMesasTexto);
                        } catch (NumberFormatException e) {
                            Snackbar.make(v, "Introduce un número válido para las mesas", Snackbar.LENGTH_SHORT).show();
                            return;
                        }

                        //Hacemos la busqueda del espacio por medio del ID conseguido previamente, para actualizarlo
                        busquedaEspacio.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if (dataSnapshot.exists()) {
                                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                        //Actualizamos el campo nMesas en la base de datos
                                        snapshot.getRef().child("nMesas").setValue(nuevoNMesas).addOnCompleteListener(task -> {
                                            if (task.isSuccessful()) {
                                                Snackbar.make(v, "El espacio se actualizó correctamente", Snackbar.LENGTH_SHORT).show();
                                                espacioActual.setnMesas(nuevoNMesas);

                                                //Refrescamos la lista
                                                notifyDataSetChanged();
                                                dialogoEspacios.dismiss();
                                            } else {
                                                Snackbar.make(v, "Error al actualizar el espacio", Snackbar.LENGTH_SHORT).show();
                                            }
                                        });
                                    }
                                } else {
                                    Snackbar.make(v, "El espacio '"+espacioActual.getNombre() + "' no existe en la base de datos", Snackbar.LENGTH_SHORT).show();
                                }
                            }
                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                Snackbar.make(v, "Error de base de datos", Snackbar.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
                dialogoEspacios.show();
            }
        });

        return convertView;
    }
}
