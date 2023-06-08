package com.rashidmayes.clairvoyance;

import com.aerospike.client.Record;
import com.google.gson.Gson;
import gnu.crypto.util.Base64;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.text.TextAlignment;
import javafx.util.Callback;
import org.apache.commons.lang.StringUtils;

import java.text.Format;
import java.util.Map;

public class NoSQLCellFactory implements Callback<CellDataFeatures<RecordRow, String>, ObservableValue<String>> {

    private TextAlignment alignment;
    private Format format;
    private String mBinName;
    private int maxBinaryLen = 512;

    private static final Gson gson = new Gson();


    public NoSQLCellFactory(String binName) {
        this.mBinName = binName;
    }

    public TextAlignment getAlignment() {
        return alignment;
    }

    public void setAlignment(TextAlignment alignment) {
        this.alignment = alignment;
    }

    public Format getFormat() {
        return format;
    }

    public void setFormat(Format format) {
        this.format = format;
    }

    @Override
    public ObservableValue<String> call(CellDataFeatures<RecordRow, String> param) {

        RecordRow recordRow = param.getValue();
        if (recordRow != null) {
            Record record = recordRow.getRecord();
            if (record != null) {

                if (record == RecordRow.LOADING_RECORD) {
                    return new SimpleStringProperty("loading...");
                } else {
                    Map<String, Object> bins = record.bins;
                    if (bins != null) {
                        Object value = bins.get(mBinName);

                        if (value != null) {
                            if (value instanceof String || value instanceof Number) {
                                return new SimpleStringProperty(value.toString());
                            } else if (value instanceof byte[]) {
                                return new SimpleStringProperty(StringUtils.abbreviate(Base64.encode((byte[]) value), maxBinaryLen));
                            } else {
                                return new SimpleStringProperty(gson.toJson(value));
                            }
                        }
                    }
                }
            }
        }
        return new SimpleStringProperty("");
    }
}
