package com.karengin.libproject.UI.view;

import com.karengin.libproject.UI.window.AddBookWindow;
import com.karengin.libproject.UI.window.EditBookWindow;
import com.karengin.libproject.dto.BookDto;
import com.karengin.libproject.service.AuthorService;
import com.karengin.libproject.service.BookService;
import com.karengin.libproject.service.GenreService;
import com.vaadin.data.provider.DataProvider;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.*;
import com.vaadin.ui.components.grid.MultiSelectionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import static java.lang.Math.toIntExact;

@SpringView(name = BooksView.NAME)
@Component(BooksView.NAME)
public class BooksView extends AbstractView<BookDto> {

    public static final String NAME = "books";

    @Autowired
    private BookService bookService;

    @Autowired
    private AuthorService authorService;

    @Autowired
    private GenreService genreService;

    @PostConstruct
    void init() {
        grid = new Grid<>(BookDto.class);
        selectionModel = (MultiSelectionModel<BookDto>) grid.setSelectionMode(Grid.SelectionMode.MULTI);
        settingSelectionModel();
        setEventListeners();

        this.addComponent(headerLayout);
        this.addComponent(grid);

        dataProvider = DataProvider.fromFilteringCallbacks(
                query -> {
                    String filter = query.getFilter().orElse(null);
                    if (filter == null) {
                        dtoList = bookService.getBooksList(null).getBody();
                    } else {
                        dtoList = bookService.getBooksByTitle(filter).getBody();
                    }
                    return dtoList.stream();
                },
                query -> {
                    String filter = query.getFilter().orElse(null);
                    return toIntExact(bookService.getBooksCount(filter).getBody());
                }
        );
        wrapper = dataProvider.withConfigurableFilter();

        grid.setDataProvider(wrapper);
        grid.setWidth("800px");
        grid.getColumn("description").setWidth(200);
    }

    @Override
    protected void setEventListeners() {
        addButton.addClickListener(clickEvent -> {
            final Window window = new AddBookWindow(bookService, authorService, genreService);
            window.addCloseListener(closeEvent -> {
                dataProvider.refreshAll();
                selectionModel.deselectAll();
            });
            getUI().addWindow(window);
        });

        editButton.addClickListener(clickEvent ->
            grid.getSelectedItems().forEach(bookDto -> {
                final Window window = new EditBookWindow(bookDto, bookService, authorService, genreService);
                window.addCloseListener(closeEvent -> {
                    dataProvider.refreshAll();
                    selectionModel.deselectAll();
                });
                getUI().addWindow(window);
            })
        );

        deleteButton.addClickListener(clickEvent -> {
            grid.getSelectedItems().forEach(bookDto ->
                    bookService.removeBook(bookDto));
            dataProvider.refreshAll();
            selectionModel.deselectAll();
            Notification.show("Selected books was deleted");
        });

        nameFilteringTextField.addValueChangeListener(valueChangeEvent -> {
            String filter = valueChangeEvent.getValue();
            if (filter.trim().isEmpty()) {
                filter = null;
            }
            wrapper.setFilter(filter);
        });
    }
}
