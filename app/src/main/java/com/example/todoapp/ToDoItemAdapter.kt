package com.example.todoapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleObserver
import androidx.recyclerview.widget.RecyclerView
import com.example.todoapp.databinding.ItemBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat

enum class Order(val value: Byte) {
    CreateDate(1),
    CreateDateCheck(2),
    CreateDatePriority(3),
    CreateDateCheckPriority(4),
    ChangeDate(5),
    ChangeDateCheck(6),
    ChangeDatePriority(7),
    ChangeDateCheckPriority(8),
    RevCreateDate(9),
    RevCreateDateCheck(10),
    RevCreateDatePriority(11),
    RevCreateDateCheckPriority(12),
    RevChangeDate(13),
    RevChangeDateCheck(14),
    RevChangeDatePriority(15),
    RevChangeDateCheckPriority(16)

}

    //adapter отвечающий за отображение элементов списка элементов recyclerView
class ToDoItemRecyclerAdapter(private val toDoItemRepository: ToDoItemRepository): RecyclerView.Adapter<ToDoItemRecyclerAdapter.ToDoItemHolder>(), LifecycleObserver {

    var sortOrder: Order = Order.CreateDate
        set(value){
            field = value
            toDoItemRepository.sortOrder = field
        }

    private var itemCount: Int = 0
        fun setItemCount(value: Int) {itemCount = value}
    //функия запрашивает число элеменов списка
    override fun getItemCount(): Int {
        return itemCount
    }

        //подкласс ViewHolder предстовляющий собой пустой элемент дела из списка
    class ToDoItemHolder(itemView: View, private val toDoItemRepository: ToDoItemRepository): RecyclerView.ViewHolder(itemView){
        private val binding = ItemBinding.bind(itemView)
        private val dateFormat = SimpleDateFormat("\nHH:mm:ss\ndd.MM.yyyy");

            //функция замены информации элемента
        fun bind(item: ToDoItem) = with(binding){
                //выставление галочки в checkBox
            checkBoxItem.isChecked = item.check
                //ClickListener сохраниющий информации об изменеии checkBox
            checkBoxItem.setOnClickListener{
                toDoItemRepository.editToDoItem(item.id, checkBoxItem.isChecked)
            }


            textViewString.maxLines = 3
            //textViewString.ellipsize = TextUtils.TruncateAt.END
            var isFullTextView = false
            itemView.setOnClickListener {
                if (isFullTextView){
                    textViewString.maxLines = 3
                    isFullTextView = false
                }
                else{
                    textViewString.maxLines = Int.MAX_VALUE
                    isFullTextView = true
                }
            }

            itemView.setOnLongClickListener {
                Toast.makeText(toDoItemRepository.context, "1212", Toast.LENGTH_SHORT).show()
                toDoItemRepository.lifecycleScope.launch {

                }



                true
            }

            //itemView.setOnTouchListener { v, event -> event  }

                //передача текста дела
            textViewString.text = item.mainText
                //передача даты создания дела
            textViewCreateDate.text = itemView.context.getString(R.string.createDate, dateFormat.format(item.itemCreateDate))
                //передача даты редоктаривония дела если она есть
            if(item.itemChangeDate == null) {
                textViewChangeDate.isVisible = false
                textViewChangeDate.text = itemView.context.getString(R.string.changeDate)
            }
            else{
                textViewChangeDate.text = itemView.context.getString(R.string.changeDate, dateFormat.format(item.itemChangeDate))
                textViewChangeDate.isVisible = true
            }
                //высталения цвета в соответствии со срочностью
            linearLayoutItem.setBackgroundResource(when(item.priority){
                Priority.LOW -> R.color.lowPriority
                Priority.USUAL -> R.color.usualPriority
                Priority.EMERGENCY -> R.color.emergencyPriority
            })


            /*itemView.setOnClickListener(){
                val swi = itemView.findViewById<Switch>(R.id.switchCheck)
                swi.isChecked = !swi.isChecked
            }//   */

        }
    }

        //создание обекта подкласса ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ToDoItemHolder {
            //создание нового отображение из файла item.xml
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item, parent, false)
        return ToDoItemHolder(view, toDoItemRepository)
    }

        //функция для замены информации элемента ViewHolder на корректную
    override fun onBindViewHolder(holder: ToDoItemHolder, position: Int) {
        holder.bind(toDoItemRepository.getToDoItemPerPosition(sortOrder, position))
    }
}