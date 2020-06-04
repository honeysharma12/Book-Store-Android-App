package activity

import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.os.AsyncTask
import androidx.core.app.ActivityCompat
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.widget.Toolbar
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.honey.bookhub.R
import okhttp3.Request
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.room.Room
import com.android.volley.Response
import com.squareup.picasso.Picasso
import database.BookDatabase
import database.BookEntity
import kotlinx.android.synthetic.main.activity_description.*
import org.json.JSONException
import org.json.JSONObject

class DescriptionActivity : AppCompatActivity() {

    lateinit var txtBookName: TextView
    lateinit var txtBookAuthor: TextView
    lateinit var txtBookPrice: TextView
    lateinit var txtBookRating: TextView
    lateinit var imgBookImage: ImageView
    lateinit var progressLayout: RelativeLayout
    lateinit var btnAddToFav: Button
    lateinit var progressBar: ProgressBar
    lateinit var txtBookDesc: TextView
    lateinit var toolbar: Toolbar

    var bookId: String? = "100"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_description)

        txtBookName = findViewById(R.id.txtBookName)
        txtBookAuthor = findViewById(R.id.txtBookAuthor)
        txtBookPrice = findViewById(R.id.txtBookPrice)
        txtBookRating = findViewById(R.id.txtBookRating)
        imgBookImage = findViewById(R.id.imgBookImage)
        txtBookDesc = findViewById(R.id.txtBookDesc)
        btnAddToFav = findViewById(R.id.btnAddToFav)
        progressLayout = findViewById(R.id.progressLayout)
        progressBar = findViewById(R.id.progressBar)

        progressBar.visibility = View.VISIBLE
        progressLayout.visibility = View.VISIBLE

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Book Details"

        if(intent != null){
            bookId = intent.getStringExtra("book_id")
        }else{
            Toast.makeText(this@DescriptionActivity , "Some Error occurred " , Toast.LENGTH_SHORT).show()
        }

        if(bookId == "100"){
            finish()
            Toast.makeText(this@DescriptionActivity , " Some error occurred" , Toast.LENGTH_SHORT).show()
        }

        val queue = Volley.newRequestQueue(this@DescriptionActivity)
        val url = "http://13.235.250.119/v1/book/get_book/"

        val jsonParams = JSONObject()
        jsonParams.put("book_id", bookId)

        val jsonRequest = object : JsonObjectRequest(Method.POST, url , jsonParams ,
             Response.Listener{
                try{
                     val success = it.getBoolean("success")

                    if(success){

                        val bookJsonObject = it.getJSONObject("book_data")
                        progressLayout.visibility = View.GONE

                        val bookImageUrl = bookJsonObject.getString("image")

                        Picasso.get().load(bookJsonObject.getString("image"))
                            .error(R.drawable.default_book_cover).into(imgBookImage)
                        txtBookName.text = bookJsonObject.getString("name")
                        txtBookAuthor.text = bookJsonObject.getString("author")
                        txtBookPrice.text = bookJsonObject.getString("price")
                        txtBookRating.text = bookJsonObject.getString("rating")
                        txtBookDesc.text = bookJsonObject.getString("description")

                        val bookEntity = BookEntity(
                            bookId?.toInt() as Int,
                            txtBookName.text.toString(),
                            txtBookAuthor.text.toString(),
                            txtBookPrice.text.toString(),
                            txtBookRating.text.toString(),
                            txtBookDesc.text.toString(),
                            bookImageUrl
                        )

                        val checkFav = DBAsyncTask(applicationContext , bookEntity , 1).execute()
                        val isFav = checkFav.get()

                        if(isFav){

                            btnAddToFav.text = "Remove From Favourites"
                            val favColor = ContextCompat.getColor(applicationContext , R.color.colorFavourite)
                            btnAddToFav.setBackgroundColor(favColor)

                        }else{

                            btnAddToFav.text = "Add To Favourites"
                            val noFavColor = ContextCompat.getColor(applicationContext , R.color.colorPrimary)
                            btnAddToFav.setBackgroundColor(noFavColor)
                        }

                        btnAddToFav.setOnClickListener{

                            if(!DBAsyncTask(applicationContext , bookEntity , 1).execute().get()){

                                val async = DBAsyncTask(applicationContext , bookEntity , 2).execute()
                                val result = async.get()

                                if(result){
                                    Toast.makeText(this@DescriptionActivity ,
                                        "Book added to favourites" ,
                                        Toast.LENGTH_SHORT)
                                        .show()
                                    btnAddToFav.text = "Remove from favourites"
                                    val favColor = ContextCompat.getColor(applicationContext , R.color.colorFavourite)
                                    btnAddToFav.setBackgroundColor(favColor)
                                }else{
                                    Toast.makeText(this@DescriptionActivity ,
                                        "Some error occurred!" ,
                                        Toast.LENGTH_SHORT)
                                        .show()
                                }
                            }else{

                                val async = DBAsyncTask(applicationContext , bookEntity , 3).execute()
                                val result = async.get()

                                if(result){
                                    Toast.makeText(this@DescriptionActivity ,
                                        "Removed from favourites" ,
                                        Toast.LENGTH_SHORT)
                                        .show()

                                    btnAddToFav.text = "Add to favourites"
                                    val noFavColor = ContextCompat.getColor(applicationContext , R.color.colorPrimary)
                                    btnAddToFav.setBackgroundColor(noFavColor)
                                }else{
                                    Toast.makeText(this@DescriptionActivity ,
                                        "Some error occurred!" ,
                                        Toast.LENGTH_SHORT)
                                        .show()
                                }
                            }
                        }
                    }else{
                        Toast.makeText(this@DescriptionActivity , "Some error occurred" ,Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception){
                    Toast.makeText(this@DescriptionActivity , "Some error occurred" ,Toast.LENGTH_SHORT).show()
                }
            },
            Response.ErrorListener{
                Toast.makeText(this@DescriptionActivity , "Volley Error $it" ,Toast.LENGTH_SHORT).show()
            }
        )
        {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String , String>()
                headers["Content-type"] = "application/json"
                headers["token"] = "4dd8eadb7acfb4"
                return headers
            }

        }
        queue.add(jsonRequest)
    }

    class DBAsyncTask(val context: Context , val bookEntity: BookEntity , val mode: Int): AsyncTask<Void , Void , Boolean>() {
        /* Mode 1 -> Check DB if the book is favourite or not
        Mode 2 -> save the book into DB as Favourites
        Mode 3 -> Remove the favourite book
         */

        val db = Room.databaseBuilder(context , BookDatabase::class.java , "books-db").build()

        override fun doInBackground(vararg params: Void?): Boolean {

            when(mode){

                1 -> {
                    val book: BookEntity? = db.bookDao().getBookById(bookEntity.book_id.toString())
                    db.close()
                    return book != null
                }

                2 -> {

                    db.bookDao().insertBook(bookEntity)
                    db.close()
                    return true
                }

                3 -> {
                    db.bookDao().deleteBook(bookEntity)
                    db.close()
                    return true

                }
            }
            return false
        }
    }
}
