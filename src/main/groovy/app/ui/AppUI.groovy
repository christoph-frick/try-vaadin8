package app.ui
import com.vaadin.annotations.Theme
import com.vaadin.annotations.Widgetset
import com.vaadin.data.*
import com.vaadin.data.converter.StringToBigDecimalConverter
import com.vaadin.data.provider.CallbackDataProvider
import com.vaadin.data.validator.RegexpValidator
import com.vaadin.icons.VaadinIcons
import com.vaadin.server.SerializablePredicate
import com.vaadin.server.VaadinRequest
import com.vaadin.shared.Registration
import com.vaadin.spring.annotation.SpringUI
import com.vaadin.ui.*
import com.vaadin.ui.renderers.NumberRenderer
import com.vaadin.ui.themes.ValoTheme
import groovy.transform.Immutable
import groovy.transform.ToString

import javax.validation.constraints.Max
import javax.validation.constraints.Min
import java.text.DecimalFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@SpringUI
@Theme('app')
@Widgetset('app')
class AppUI extends UI {

    @Override
    protected void init(VaadinRequest vaadinRequest) {
        Grid grid
        def binder = new Binder<Item>(Item)

        def binderStatusLabel = new Label().with {
            addStyleName(ValoTheme.LABEL_H1)
            it
        }

        def nameField = new TextField("Name")
        def nameStatusLabel = new Label()

        def priceField = new TextField().with {
            caption = "Price"
            it
        }
        def priceStatusLabel = new Label()

        def durationField = new DurationField().with {
            caption = "Duration"
            it
        }
        def durationStatusLabel = new Label()

        def geoField = new GeoPointField().with{
            caption = "Geo"
            it
        }

        // validator of the whole object
        binder.statusLabel = binderStatusLabel
        binder.withValidator({ Item item ->
            item?.price > item?.duration?.amount
        } as SerializablePredicate, "Price must be higher than duration")

        // complex status handler
        binder.forField(nameField).
                asRequired("Must have a value").
                withValidator(new RegexpValidator("Only uppercase allowed", /[A-Z]+/)).
                withValidationStatusHandler({ BindingValidationStatus status ->
                    nameStatusLabel.visible = status.status != BindingValidationStatus.Status.UNRESOLVED
                    if (status.status != BindingValidationStatus.Status.UNRESOLVED) {
                        nameStatusLabel.value = status.error ? status.message.get() : "OK"
                        nameStatusLabel.setStyleName(ValoTheme.LABEL_FAILURE, status.error)
                        nameStatusLabel.setStyleName(ValoTheme.LABEL_SUCCESS, !status.error)
                    }
                } as BindingValidationStatusHandler).
                bind 'name'

        // lesson learned: calls must be chained
        // withConverter returns a new/wrapped object -- using groovy's `with` will throw it away and the converter is not in place
        binder.forField(priceField).
                withStatusLabel(priceStatusLabel).
                withConverter(new StringToBigDecimalConverter("Please enter a number")).
                bind 'price'

        binder.forField(durationField).
                withValidator({ Duration d -> d.amount > 0 } as SerializablePredicate, "Must have positive duration").
                withStatusLabel(durationStatusLabel).
                bind 'duration'

        binder.forField(geoField).
                withValidator({ GeoPoint x -> x.lon > 0 && x.lat > 0 } as SerializablePredicate, "Must have positive geo point").
                bind 'geoPoint'

        setContent(
                new HorizontalLayout(
                        // grid
                        new VerticalLayout(
                                grid = new Grid().with {
                                    setDataProvider(
                                            // dummy callback provider
                                            new CallbackDataProvider<Item, ?>(
                                                    { query ->
                                                        [
                                                                new Item(1L, "Foo", 42.0G, LocalDateTime.now(), new Duration(10.0d, DurationUnit.MINUTES), new GeoPoint(-200.0G,-100.0G)),
                                                                new Item(2L, "Bar", 23.0G, LocalDateTime.now(), new Duration(2.0d, DurationUnit.HOURS), new GeoPoint(0.0G,0.0G)),
                                                                new Item(3L, "Baz", 666.0G, LocalDateTime.now(), new Duration(3.0d, DurationUnit.DAYS), new GeoPoint(0.0G,0.0G)),
                                                        ].stream()
                                                    } as CallbackDataProvider.FetchCallback,
                                                    { query -> 3 } as CallbackDataProvider.CountCallback,
                                            )
                                    )
                                    addColumn({ Item item -> item.name } as ValueProvider).
                                            setCaption("Name").
                                            setId('name')
                                    // format by renderer
                                    addColumn({ Item item -> item.price } as ValueProvider, new NumberRenderer(new DecimalFormat("0.00"), "-")).
                                            setCaption("Price").
                                            setId('price')
                                    // simple direct formatting
                                    addColumn({ Item item -> item.dateTime.format(DateTimeFormatter.ofPattern('yyyy MM dd, hh:mm:ss')) } as ValueProvider).
                                            setCaption("Date").
                                            setId('date')
                                    addColumn({ Item item -> "${item.duration.amount} ${item.duration.unit}" } as ValueProvider).
                                            setCaption("Duration").
                                            setId('duration')
                                    addColumn({ Item item -> "${item.geoPoint}" } as ValueProvider).
                                            setCaption("Geo").
                                            setId('geoPoint')
                                    // single selection handler
                                    asSingleSelect().addValueChangeListener({ HasValue.ValueChangeEvent<Item> event ->
                                        Notification.show("Loading $event.value")
                                        binder.setBean(event.value)
                                        binder.validate()
                                    } as HasValue.ValueChangeListener<Item>)
                                    setSizeFull()
                                    it
                                },
                                new Button().with {
                                    caption = "Refresh"
                                    addClickListener { grid.dataProvider.refreshAll() }
                                    it
                                }
                        ).with {
                            setExpandRatio(grid, 1.0f)
                            it
                        },
                        // form
                        new VerticalLayout(
                                binderStatusLabel,
                                nameField, nameStatusLabel,
                                priceField, priceStatusLabel,
                                durationField, durationStatusLabel,
                                geoField,
                                new Button().with {
                                    caption = "Save"
                                    icon = VaadinIcons.CHECK
                                    addClickListener {
                                        if (binder.writeBeanIfValid(binder.bean)) {
                                            Notification.show("Saved $binder.bean")
                                        } else {
                                            Notification.show("There are errors")
                                        }
                                    }
                                    it
                                }
                        ).with {
                            setSpacing(true)
                            it
                        }
                )
        )
    }

}

// dummy objects for the grid and the form

@ToString(includeNames = true)
class Item {
    Long id
    String name
    BigDecimal price
    LocalDateTime dateTime
    Duration duration
    GeoPoint geoPoint

    Item(Long id, String name, BigDecimal price, LocalDateTime dateTime, Duration duration, GeoPoint geoPoint) {
        this.id = id
        this.name = name
        this.price = price
        this.dateTime = dateTime
        this.duration = duration
        this.geoPoint = geoPoint
    }
}

@Immutable
@ToString(includeNames = true)
class Duration {
    Double amount
    DurationUnit unit
}

enum DurationUnit {
    MINUTES, HOURS, DAYS
}

@ToString(includeNames = true)
class GeoPoint {

    @Min(value=-180l)
    @Max(value=180l)
    BigDecimal lon

    @Min(value=-90l)
    @Max(value=90l)
    BigDecimal lat

    GeoPoint(BigDecimal lon, BigDecimal lat) {
        this.lon = lon
        this.lat = lat
    }
}

/*
 * Composite Fields
 *
 * Easy to build with a CustomField and reasonably easy to get right -- if not for the new "immediate" behavior.
 * Regular fields (like e.g. TextField) report basically (debounced) each key strokes.  Validation in the binder
 * updates all the time.  This becomes more apparent when using status labels.
 */

// composite custom field, with the value change listeners set up manually
class DurationField extends CustomField<Duration> {

    final TextField amountField
    final NativeSelect<DurationUnit> unitField

    private final HasValue.ValueChangeListener vcl = { HasValue.ValueChangeEvent e ->
        // blindly fire the change event
        // can not use setValue here, because it will use getValue to see, if there is change, so there never is
        fireEvent(createValueChange(value, e.userOriginated))
    } as HasValue.ValueChangeListener

    DurationField() {
        amountField = new TextField()
        unitField = new NativeSelect<DurationUnit>()
        unitField.items = EnumSet.allOf(DurationUnit)
        // wire the change listener on each field
        // TODO: do we, and if when, have to take care about deregistriation
        amountField.addValueChangeListener vcl
        unitField.addValueChangeListener vcl
    }

    @Override
    protected Component initContent() {
        new HorizontalLayout(amountField, unitField).with{
            setSpacing(true)
            setMargin(false)
            it
        }
    }

    // @Override
    protected void doSetValue(Duration value) {
        amountField.value = String.valueOf(value.amount)
        unitField.value = value.unit
    }

    @Override
    Duration getValue() {
        // just ignore anything, that could go wrong here...
        if (amountField.value && unitField.value) {
            return new Duration(amountField.value.toDouble(), unitField.value)
        }
        return null
    }

}

// composite custom field using internally a binder and pass the value change listener just down to it
class GeoPointField extends CustomField<GeoPoint> {

    final Binder<GeoPoint> binder
    final TextField lonField, latField

    GeoPointField() {
        lonField = new TextField("lon")
        latField = new TextField("lat")
        binder = new BeanValidationBinder<GeoPoint>(GeoPoint)
        binder.forField(lonField).
                withConverter(new StringToBigDecimalConverter("Only numbers")).
                bind('lon')
        binder.forField(latField).
                withConverter(new StringToBigDecimalConverter("Only numbers")).
                bind('lat')
    }

    @Override
    protected Component initContent() {
        new HorizontalLayout(
                lonField,
                latField,
        ).with{
            setSpacing(true)
            setMargin(false)
            it
        }
    }

    // @Override
    protected void doSetValue(GeoPoint value) {
        binder.setBean(value)
    }

    @Override
    GeoPoint getValue() {
        binder.getBean()
    }

    @Override
    Registration addValueChangeListener(HasValue.ValueChangeListener<GeoPoint> listener) {
        // TODO: seems to work fine, but should we register in super too?  Is there a Registration, that can wrap multiple registrations?
        return binder.addValueChangeListener(listener)
    }

}
