package com.example.cocinegocios.Adaptadores;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.cocinegocios.Clases.ProductoComanda;
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

import java.text.DecimalFormat;
import java.util.ArrayList;

/**
 * Clase con funcion de adaptador para las listas que contengan productos de las comandas del negocio en la pantalla de creación.
 * <p>
 * Esta clase o adaptador contiene la lógica necesaria para mostrar los productos de las comandas que haya en el negocio donde él usuario esté actualmente de la pantalla de creación.
 * <p>
 * Autor: [Juan Ramón de León Martín]
 * Fecha: [3/12/2024]
 */

public class AdaptadorListaProductosComandaCreacion extends ArrayAdapter<ProductoComanda> {

    private ArrayList<ProductoComanda> lista;
    private Context mContext;

    private StorageReference mStorage;

    private String correoUsuario;

    private Bundle negocioSeleccionado;

    /**
     * Constructor del adaptador de la lista de productos de la comanda en la creacion de comandas.
     * <p>
     * Este constructor inicializa el adaptador con el contexto, la lista de productos de la comanda, y el nombre del negocio en el que estamos actualmente, en el sitio de creación de comandas.
     *
     * @param contexto El contexto de la actividad o fragmento donde se utiliza el adaptador.
     * @param lista La lista de objetos de tipo ProductoComanda que se mostrarán en la vista.
     * @param nombreNegocio El nombre del negocio en el que estamos actualmente.
     */
    public AdaptadorListaProductosComandaCreacion(Context contexto, ArrayList<ProductoComanda> lista, String nombreNegocio) {
        super(contexto, R.layout.elemento_producto_comanda_creacion, lista);
        this.lista = lista;
        this.mContext = contexto;
        this.mStorage = FirebaseStorage.getInstance().getReference();
        this.correoUsuario = nombreNegocio;
    }

    /**
     * Metodo para inicializar el contenido del listado de productos dentro de la comanda actual, en la pantalla de creacion de comandas.
     * <p>
     * Esta clase inicializa el contenido de cada uno de los productos de la lista de productos de la comanda actual, en la pantalla de creacion de comandas.
     */
    public static class ViewHolder {
        TextView nombreProducto;
        TextView precioProducto;
        Button botonEliminarProducto;
        Button botonCambiarCantidadProducto;
    }

    /**
     * Metodo para dar utilidad a los productos durante la creacion de la comanda
     * <p>
     * Esta clase coge y asocia los contenidos de cada elemento productoComanda y los asocia con los del 'elemento_producto_comanda_creacion' mediante los ViewHolder. El ViewHolder se crea si no hay uno previamente,en el
     * caso contrario se reutiliza. Luego se pone los datos correspondientes a los TextView y se crea la logica del botón cambiar cantidad producto para poder cambiar la cantidad del producto seleccionado
     *
     * @param position La posición del elemento en la lista que se está procesando.
     * @param convertView La vista del elemento productosComanda en el que se trabaja.
     * @param parent Los datos usados para conocer las dimensiones y las propiedades del contenedor.
     *
     * @return convertView La vista convertida en viewwHolder para mostrar el producto.
     */
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        AdaptadorListaProductosComandaCreacion.ViewHolder viewHolder;

        negocioSeleccionado = new Bundle();

        //Reutilizar la vista existente si es posible
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            convertView = inflater.inflate(R.layout.elemento_producto_comanda_creacion, parent, false);

            //Crear un nuevo ViewHolder
            viewHolder = new AdaptadorListaProductosComandaCreacion.ViewHolder();
            viewHolder.nombreProducto = convertView.findViewById(R.id.nombreProducto);
            viewHolder.precioProducto = convertView.findViewById(R.id.precioProducto);
            viewHolder.botonEliminarProducto = convertView.findViewById(R.id.botonEliminarProducto);
            viewHolder.botonCambiarCantidadProducto = convertView.findViewById(R.id.botonCambiarCantidadProducto);

            convertView.setTag(viewHolder);
        } else {
            //Si la vista ya existe, reutilizar el ViewHolder
            viewHolder = (AdaptadorListaProductosComandaCreacion.ViewHolder) convertView.getTag();
        }

        //Obtener los datos del los productos de la comanda actual
        ProductoComanda productoActual = lista.get(position);

        viewHolder.nombreProducto.setText(productoActual.getNombre());
        DecimalFormat formato = new DecimalFormat("0.00");
        viewHolder.precioProducto.setText(formato.format(productoActual.getPrecioUnidad()) + " €");

        viewHolder.botonCambiarCantidadProducto.setText(String.valueOf(productoActual.getCantidad()));

        /*
         * Se da lógica al boton que sirve para mostrar y cambiar la cantidad del producto elegido de la comanda
         */
        viewHolder.botonCambiarCantidadProducto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Dialog dialogoCambiarCantidadProducto = new Dialog(mContext);
                dialogoCambiarCantidadProducto.setContentView(R.layout.dialogo_editar_cantidad_producto);
                dialogoCambiarCantidadProducto.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

                EditText cantidadDelProducto = dialogoCambiarCantidadProducto.findViewById(R.id.cantidadDelProductoPropuesto);
                cantidadDelProducto.setText(String.valueOf(productoActual.getCantidad()));

                String claveCategoria = productoActual.getId();
                DatabaseReference databaseReferenceProductosComanda = FirebaseDatabase.getInstance().getReference("productosComanda");
                Query busquedaProductosComanda = databaseReferenceProductosComanda.orderByChild("id").equalTo(claveCategoria);

                /*
                 * Se da lógica al boton que sirve para cambiar el numero de veces del producto de la comanda, or medio de la cantidad introducida anteriormente
                 */
                Button botonEditar = dialogoCambiarCantidadProducto.findViewById(R.id.botonCambiarCantidadProducto);
                botonEditar.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        busquedaProductosComanda.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if (dataSnapshot.exists()) {
                                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {

                                        String nuevaCantidadTexto = cantidadDelProducto.getText().toString().trim();

                                        //Convertimos de un String a un Int la nueva cantidad del texto
                                        if (nuevaCantidadTexto.isEmpty()) {
                                            Snackbar.make(v, "El número de mesas no puede estar vacío", Snackbar.LENGTH_SHORT).show();
                                            return;
                                        }

                                        int nuevaCantidad;
                                        try {
                                            nuevaCantidad = Integer.parseInt(nuevaCantidadTexto);
                                        } catch (NumberFormatException e) {
                                            Snackbar.make(v, "Introduce un número válido para las mesas", Snackbar.LENGTH_SHORT).show();
                                            return;
                                        }
                                        // Actualizamos el campo cantidad del producto
                                        snapshot.getRef().child("cantidad").setValue(nuevaCantidad).addOnCompleteListener(task -> {
                                            if (task.isSuccessful()) {
                                                Snackbar.make(v, "El espacio se actualizó correctamente", Snackbar.LENGTH_SHORT).show();
                                                productoActual.setCantidad(nuevaCantidad); // Actualizamos localmente
                                                notifyDataSetChanged(); // Refrescamos la lista
                                                dialogoCambiarCantidadProducto.dismiss(); // Cerramos el diálogo
                                            } else {
                                                Snackbar.make(v, "Error al actualizar el espacio", Snackbar.LENGTH_SHORT).show();
                                            }
                                        });
                                    }
                                } else {
                                    Snackbar.make(v, "El espacio '"+productoActual.getNombre() + "' no existe en la base de datos", Snackbar.LENGTH_SHORT).show();
                                }
                            }
                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                Snackbar.make(v, "Error de base de datos", Snackbar.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
                dialogoCambiarCantidadProducto.show();
            }
        });

        /*
         * Se da lógica al boton que sirve para eliminar el productoComanda de esta lista, y en si borrarlo
         */
        viewHolder.botonEliminarProducto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String claveCategoria = productoActual.getId();
                DatabaseReference databaseReferenceProductosComanda = FirebaseDatabase.getInstance().getReference("productosComanda");
                Query busquedaProductosComanda = databaseReferenceProductosComanda.orderByChild("id").equalTo(claveCategoria);

                //Se busca el productoComanda para que sea borrado correctamente y que exista
                busquedaProductosComanda.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                snapshot.getRef().removeValue();
                            }
                            lista.remove(position);
                            notifyDataSetChanged();
                        } else {
                            Snackbar.make(v, "El producto de la comanda '"+productoActual.getNombre() + "' no existe en la base de datos", Snackbar.LENGTH_SHORT).show();
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Snackbar.make(v, "Error de base de datos", Snackbar.LENGTH_SHORT).show();
                    }
                });
            }
        });

        return convertView;
    }
}
