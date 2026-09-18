package ir.aeliux.usbtoolkit.viewmodel;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import ir.aeliux.usbtoolkit.data.GadgetState;

public class FilePickerViewModel extends ViewModel {
    private final MutableLiveData<String> currentDirectory = new MutableLiveData<>("");
    private final MutableLiveData<List<String>> filePaths = new MutableLiveData<>(new ArrayList<>());

    public MutableLiveData<String> getCurrentDirectory() {
        return currentDirectory;
    }

    public void setCurrentDirectory(String value) {
        if (currentDirectory.getValue() != null && value.equals(currentDirectory.getValue())) return;
        currentDirectory.setValue(value);
    }

    public void setDefaultDirectory(String value) {
        if (Objects.requireNonNull(currentDirectory.getValue()).isEmpty()) {
            setCurrentDirectory(value);
        }
    }

    public MutableLiveData<List<String>> getFilePaths() {
        return filePaths;
    }

    public void setFilePaths(List<String> value) {
        if (filePaths.getValue() != null && value.equals(filePaths.getValue())) return;

        filePaths.setValue(value);
    }
}
