package com.example.cocinegocios.Adaptadores;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.cocinegocios.Clases.Categoria;
import com.example.cocinegocios.Clases.Producto;
import com.example.cocinegocios.MenuAdministrador;
import com.example.cocinegocios.R;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

/**
 * Clase con funcion de adaptador para las listas que contengan categorias del negocio.
 * <p>
 * Esta clase o adaptador contiene la lógica necesaria para mostrar las categorias que haya en el negocio donde él usuario esté actualmente.
 * <p>
 * Autor: [Juan Ramón de León Martín]
 * Fecha: [3/12/2024]
 */

public class AdaptadorListaCategorias extends ArrayAdapter<Categoria> {

    private ArrayList<Categoria> lista;

    private Context mContext;

    private String rol;

    /**
     * Constructor del adaptador de la lista de categorías.
     * <p>
     * Este constructor inicializa el adaptador con el contexto, la lista de categorías, y el rol del usuario.
     *
     * @param contexto El contexto de la actividad o fragmento donde se utiliza el adaptador.
     * @param lista La lista de objetos de tipo Categoria que se mostrarán en la vista.
     * @param rol El rol del usuario actual, utilizado para ajustar la funcionalidad según permisos.
     */
    public AdaptadorListaCategorias(Context contexto, ArrayList<Categoria> lista, String rol) {
        super(contexto, R.layout.elemento_categorias, lista);
        this.lista = lista;
        this.mContext = contexto;
        this.rol = rol;
    }

    /**
     * Metodo para inicializar el contenido del listado de categorias.
     * <p>
     * Esta clase inicializa el contenido de cada una de las categorias de la lista de categorias.
     */
    public static class ViewHolder {
        TextView textoCategoria;
        Button botonEliminarCategoria;
    }


    /**
     * Metodo para dar utilidad a las categorias
     * <p>
     * Esta clase coge y asocia los contenidos de cada elemento categoria y los asocia con los del 'elemento_categoria' mediante los ViewHolder. El ViewHolder se crea si no hay uno previamente,
     * en el caso contrario se reutiliza. Luego se pone los datos correspondientes al TextView y se crea la logica del botón eliminar la categoria, eliminando primero los productos de la
     * categoria, y despues se elimina la categoria correspondiente.
     *
     * @param position La posición del elemento en la lista que se está procesando.
     * @param convertView La vista del elemento categoria en el que se trabaja.
     * @param parent Los datos usados para conocer las dimensiones y las propiedades del contenedor.
     *
     * @return convertView La vista convertida en viewwHolder para mostrar la categoria.
     */
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent){
        ViewHolder viewHolder;

        //Reutilizar la vista existente si es posible
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            convertView = inflater.inflate(R.layout.elemento_categorias, parent, false);

            //Crear un nuevo ViewHolder
            viewHolder = new AdaptadorListaCategorias.ViewHolder();
            viewHolder.textoCategoria = convertView.findViewById(R.id.textoCategoria);
            viewHolder.botonEliminarCategoria = convertView.findViewById(R.id.botonEliminarCategoria);

            convertView.setTag(viewHolder);
        } else {
            //Si la vista ya existe, reutilizar el ViewHolder
            viewHolder = (AdaptadorListaCategorias.ViewHolder) convertView.getTag();
        }

        // Obtener los datos de la categoria actual y settearlos
        Categoria categoriaActual = lista.get(position);
        viewHolder.textoCategoria.setText(categoriaActual.getNombreCategoria());

        //Comprueba el rol actual del usuario, porque solo los administradores pueden crear y borrar categorias
        if (rol.equals("Administrador")) {

            /*
             * Se da lógica al boton eliminar categoria, que elimina la categoria seleccionada pero antes sus productos son borrados para no causar interferencias
             */
            viewHolder.botonEliminarCategoria.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String claveCategoria = categoriaActual.getNombreCategoria();
                    DatabaseReference databaseReferenceProductos = FirebaseDatabase.getInstance().getReference("productos");

                    //Primero eliminamos los productos de la categoría específica
                    databaseReferenceProductos.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            for (DataSnapshot productoSnapshot : dataSnapshot.getChildren()) {
                                Producto producto = productoSnapshot.getValue(Producto.class);
                                //De entre todos los productos encontrados, los que coincidan su categoria con la de la categoria seleccionada son eliminados mediante este filtro
                                if (producto != null && producto.getCategoria().equals(claveCategoria)) {
                                    //Eliminar solo el producto específico
                                    productoSnapshot.getRef().removeValue();
                                }
                            }

                            //Luego de eliminar los productos, eliminar la categoría de la lista
                            DatabaseReference categoriaRef = FirebaseDatabase.getInstance().getReference("categorias").child(categoriaActual.getClaveCategoria());

                            categoriaRef.removeValue().addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    //Actualizar la lista local y notificar al adaptador
                                    lista.remove(position);
                                    notifyDataSetChanged();
                                    if (mContext instanceof MenuAdministrador) {
                                        ((MenuAdministrador) mContext).actualizarListaProductos();
                                    }

                                    Snackbar.make(v, "Categoría y sus productos eliminados", Snackbar.LENGTH_SHORT).show();
                                } else {
                                    Snackbar.make(v, "Error al eliminar la categoría", Snackbar.LENGTH_SHORT).show();
                                }
                            });
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Snackbar.make(v, "Error al acceder a los productos", Snackbar.LENGTH_SHORT).show();
                        }
                    });
                }
            });

            //En el caso de que el rol del usuario no sea el de administrador, no se muestra el botón para eliminar categorias
        } else if (rol.equals("Camarero")) {
            viewHolder.botonEliminarCategoria.setVisibility(convertView.GONE);
        } else {
            viewHolder.botonEliminarCategoria.setVisibility(convertView.GONE);
        }
        return convertView;
    }
}