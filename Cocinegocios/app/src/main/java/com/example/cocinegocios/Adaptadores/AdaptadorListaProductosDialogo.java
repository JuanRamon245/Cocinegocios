package com.example.cocinegocios.Adaptadores;

import android.content.Context;
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
import com.example.cocinegocios.Clases.Producto;
import com.example.cocinegocios.Clases.ProductoComanda;
import com.example.cocinegocios.R;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.DecimalFormat;
import java.util.ArrayList;

/**
 * Clase con funcion de adaptador para las listas que contengan productos y sean añadidos a algo.
 * <p>
 * Esta clase o adaptador contiene la lógica necesaria para mostrar los productos que haya en el negocio donde él usuario esté actualmente, para poder añadir en la comanda
 * <p>
 * Autor: [Juan Ramón de León Martín]
 * Fecha: [3/12/2024]
 */

public class AdaptadorListaProductosDialogo extends ArrayAdapter<Producto> {

    private ArrayList<Producto> lista;
    private Context mContext;

    private String correoUsuario, idComanda;

    private String id, pedidoAsociado, idProducto, nombre, estado;
    private int cantidad;
    private Double precioUnidad;

    private StorageReference mStorage;

    private Bundle negocioSeleccionado;

    /**
     * Constructor del adaptador de la lista de productos para añadir a la comanda
     *
     * Este constructor inicializa el adaptador con el contexto, la lista de productos de la comanda, el nombre del negocio actual y el ID de la comanda actual.
     *
     * @param contexto El contexto de la actividad o fragmento donde se utiliza el adaptador.
     * @param lista La lista de objetos de tipo ProductoComanda que se mostrarán en la vista.
     * @param nombreNegocio El nombre del negocio en el que estamos actualmente.
     * @param idComanda El ID de la comanda del negocio en el que estamos actualmente;
     */
    public AdaptadorListaProductosDialogo(Context contexto, ArrayList<Producto> lista, String nombreNegocio, String idComanda) {
        super(contexto, R.layout.elemento_producto_mostrar, lista);
        this.lista = lista;
        this.mContext = contexto;
        this.mStorage = FirebaseStorage.getInstance().getReference();
        this.correoUsuario = nombreNegocio;
        this.idComanda = idComanda;
    }

    /**
     * Metodo para inicializar el contenido del listado de productos para añadir a la comanda.
     *
     * Esta clase inicializa el contenido de cada uno de los productos de la lista de productos para poder ser añadidos a la comanda.
     */
    public static class ViewHolder {
        TextView nombreProducto;
        TextView precioProducto;
        ImageView imagenProducto;
        Button botonAñadirProductoComanda;
    }

    /**
     * Metodo para dar utilidad a los productos en el dialogo del menú
     *
     * Esta clase coge y asocia los contenidos de cada elemento producto y los asocia con los del 'elemento_producto_mostrar' mediante los ViewHolder. El ViewHolder se crea si no hay uno previamente,en el
     * caso contrario se reutiliza. Luego se pone los datos correspondientes a los TextView y se crea la logica del botón añadir producto comanda, para poder añadir el producto del menú a la comanda actual
     *
     * @param position La posición del elemento en la lista que se está procesando.
     * @param convertView La vista del elemento productos en el que se trabaja.
     * @param parent Los datos usados para conocer las dimensiones y las propiedades del contenedor.
     *
     * @return convertView La vista convertida en viewwHolder para mostrar el producto.
     */
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        AdaptadorListaProductosDialogo.ViewHolder viewHolder;

        negocioSeleccionado = new Bundle();

        //Reutilizar la vista existente si es posible
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            convertView = inflater.inflate(R.layout.elemento_producto_mostrar, parent, false);

            //Crear un nuevo ViewHolder
            viewHolder = new AdaptadorListaProductosDialogo.ViewHolder();
            viewHolder.nombreProducto = convertView.findViewById(R.id.nombreProducto);
            viewHolder.precioProducto = convertView.findViewById(R.id.precioProducto);
            viewHolder.imagenProducto = convertView.findViewById(R.id.imagenProducto);
            viewHolder.botonAñadirProductoComanda = convertView.findViewById(R.id.botonAñadirProductoComanda);

            convertView.setTag(viewHolder);
        } else {
            //Si la vista ya existe, reutilizar el ViewHolder
            viewHolder = (AdaptadorListaProductosDialogo.ViewHolder) convertView.getTag();
        }

        Producto productoActual = lista.get(position);

        viewHolder.nombreProducto.setText(productoActual.getNombre());
        DecimalFormat formato = new DecimalFormat("0.00");
        viewHolder.precioProducto.setText(formato.format(productoActual.getPrecio()) + " €");

        //Obtener la imagen desde Firebase Storage
        String enlaceImagen = productoActual.getFotoCodificada();
        String direcciónReal = enlaceImagen.replace("gs://negocios-de-cocinas.appspot.com/", "");
        StorageReference imageRef = mStorage.child(direcciónReal);

        //Limpiar la imagen anterior mientras se carga la nueva
        viewHolder.imagenProducto.setImageDrawable(null);

        //Cargar la imagen usando Glide
        imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
            Glide.with(mContext)
                    .load(uri)
                    .into(viewHolder.imagenProducto);
        }).addOnFailureListener(exception -> {
            Log.e("STORAGE", "Error al obtener la URL de la imagen", exception);
        });

        View finalConvertView = convertView;

        /*
         * Se da lógica al boton que sirve para añadir un producto a la comanda
         */
        viewHolder.botonAñadirProductoComanda.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatabaseReference databaseReferenceCreacion = FirebaseDatabase.getInstance("https://negocios-de-cocinas-default-rtdb.europe-west1.firebasedatabase.app/").getReference();
                DatabaseReference productosComandaReferencia = databaseReferenceCreacion.child("productosComanda");

                pedidoAsociado = idComanda;
                cantidad = 1;
                nombre = productoActual.getNombre();
                idProducto =productoActual.getId();
                id = pedidoAsociado+"*"+nombre;
                precioUnidad = productoActual.getPrecio();
                estado = "enProduccion";

                //Se busca el producto para poderlo añadir en el caso de que exista, pro el metodo de crear un productoComanda
                productosComandaReferencia.child(id).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            Snackbar.make(v, "El producto '" + nombre + "' existe en la base de datos", Snackbar.LENGTH_SHORT).show();
                        } else {
                            ProductoComanda nuevoProductoComanda = new ProductoComanda(id, pedidoAsociado, idProducto, cantidad, nombre, precioUnidad, estado);
                            productosComandaReferencia.child(id).setValue(nuevoProductoComanda).addOnSuccessListener(aVoid -> {
                                // Mostrar mensaje de éxito
                                Snackbar.make(v, "Producto registrado", Snackbar.LENGTH_SHORT).show();
                            });
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Snackbar.make(v, "Error de conexión con la base de datos", Snackbar.LENGTH_SHORT).show();
                    }
                });
            }
        });
        return convertView;
    }
}