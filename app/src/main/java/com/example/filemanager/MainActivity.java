package com.example.filemanager;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    //buttons
    private Button button1;
    private Button button2;
    private Button button3;
    private Button button4;
    private Button button5;

    //listview
    private ListView listView;

    //text adapter
    private TextAdapter textAdapter1;

    private File[] files;
    private List<String> filesList;
    private int filesFoundCount;
    //String
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout1);

    }

    class TextAdapter extends BaseAdapter {

        private List<String> data = new ArrayList<>();

        private boolean[] select;
        public void setData(List<String> data) {
            if(data != null) {
                this.data.clear();
                if(data.size() > 0) {
                    this.data.addAll(data);
                }
                notifyDataSetChanged();
            }
        }

        void setSelect(boolean[] select) {
            if (select != null) {
                this.select = new boolean[select.length];
                for (int i=0; i<select.length; i++){
                    this.select[i]=select[i];
                }
                notifyDataSetChanged();
            }
        }
        @Override
        public int getCount() {
            return data.size();
        }

        @Override
        public String getItem(int position) {
            return data.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if(convertView == null) {
                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item, parent, false);
                convertView.setTag(new ViewHolder((TextView) convertView.findViewById(R.id.textItem)) );
            }
            ViewHolder holder = (ViewHolder) convertView.getTag();
            final String item = getItem(position);
            holder.info.setText(item.substring(item.lastIndexOf('/')+1));

            if (select!=null){
                if (select[position]) {
                   holder.info.setBackgroundColor(Color.argb(100,8,8,8));
                } else {
                    holder.info.setBackgroundColor(Color.WHITE);
                }
            }
            return convertView;
        }

        class ViewHolder{
            TextView info;

            ViewHolder(TextView info) {
                this.info = info;
            }
        }
    }

    private static final int REQUEST_PERMISSIONS = 123;

    private static final String[] PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private  static final int PERMISSIONS_COUNT = 2;

    @RequiresApi(api = Build.VERSION_CODES.M)
    private boolean arePermissionsDenied() {
            int p = 0;
            while (p < PERMISSIONS_COUNT) {
                if (checkSelfPermission(PERMISSIONS[p]) != PackageManager.PERMISSION_GRANTED) {
                    return true;
                }
                p++;
            }
        return false;
    }

    private boolean[] select;
    private Button refreshButton;
    private File dir;
    private String currentPath;
    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && arePermissionsDenied()) {
            requestPermissions(PERMISSIONS, REQUEST_PERMISSIONS);
            return;
        }
        if (!isFMInitialized) {
            currentPath = String.valueOf(Environment.
                    getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS));
            final String rootPath = currentPath.substring(0,currentPath.lastIndexOf('/'));
            dir = new File(currentPath);
            files = dir.listFiles();
            final TextView pathOutput = findViewById(R.id.pathOutput);
            pathOutput.setText(currentPath.substring(currentPath.lastIndexOf('/')+1));
            filesFoundCount = files.length;
            listView = findViewById(R.id.fListView);
            textAdapter1 = new TextAdapter();
            listView.setAdapter(textAdapter1);

            filesList = new ArrayList<>();
            for (int i = 0; i < filesFoundCount; i++) {
                filesList.add(String.valueOf(files[i].getAbsoluteFile()));
            }

            textAdapter1.setData(filesList);

            select = new boolean[files.length];

            refreshButton = findViewById(R.id.refresh);
            refreshButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    files = dir.listFiles();
                    filesFoundCount = files.length;
                    filesList.clear();
                    for (int i = 0; i < filesFoundCount; i++) {
                        filesList.add(String.valueOf(files[i].getAbsoluteFile()));
                    }
                    textAdapter1.setData(filesList);
                }
            });

            final Button goBackButton = findViewById(R.id.goBack);
            goBackButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (currentPath.equals(rootPath)) {
                        return;
                    }
                    currentPath = currentPath.substring(0,currentPath.lastIndexOf('/'));
                    dir = new File(currentPath);
                    pathOutput.setText(currentPath.substring(currentPath.lastIndexOf('/')+1));
                    refreshButton.callOnClick();
                }
            });

            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    currentPath = files[position].getAbsolutePath();
                    dir = new File(currentPath);
                    pathOutput.setText(currentPath.substring(currentPath.lastIndexOf('/')+1));
                    refreshButton.callOnClick();
                }
            });

            listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                    select[position] = !select[position];
                    textAdapter1.setSelect(select);
                    boolean atLeastOne = false;
                    for (boolean aSelction : select) {
                        if (aSelction){
                            atLeastOne = true;
                            break;
                        }
                    }
                    if (atLeastOne) {
                        findViewById(R.id.bottombar).setVisibility(View.VISIBLE);
                    }else {
                        findViewById(R.id.bottombar).setVisibility(View.GONE);
                    }
                    return false;
                }
            });

            button1 = findViewById(R.id.button1);
            button2 = findViewById(R.id.button2);
            button3 = findViewById(R.id.button3);
            button4 = findViewById(R.id.button4);
            button5 = findViewById(R.id.button5);

            button1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final AlertDialog.Builder deleteDialog = new AlertDialog.Builder(MainActivity.this);
                    deleteDialog.setTitle("Delete");
                    deleteDialog.setMessage("Are you sure you want to delete this?");
                    deleteDialog.setPositiveButton("Yes", (dialog, which) -> {
                        for (int i=0; i<files.length; i++) {
                            if (select[i]) {
                                deleteFileFolder(files[i]);
                                select[i] = false;
                            }
                        }
                        refreshButton.callOnClick();
                    });
                    deleteDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });
                    deleteDialog.show();
                }
            });

            final Button createNewFolder = findViewById(R.id.newFolder);
            createNewFolder.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final AlertDialog.Builder newFolderDialog = new AlertDialog.Builder(MainActivity.this);
                    newFolderDialog.setTitle("New Folder");
                    final EditText input = new EditText(MainActivity.this);
                    input.setInputType(InputType.TYPE_CLASS_TEXT);
                    newFolderDialog.setView(input);
                    newFolderDialog.setPositiveButton("Ok",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    final File newFolder = new File(currentPath +"/"+input.getText());
                                    if (!newFolder.exists()) {
                                        newFolder.mkdir();
                                        refreshButton.callOnClick();
                                    }
                                }
                            });
                    newFolderDialog.setNegativeButton("Cancel",
                            new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });
                    newFolderDialog.show();
                }
            });

            isFMInitialized=true;
        } else {
            refreshButton.callOnClick();
        }
    }

    private void deleteFileFolder(File fileFold) {
        if(fileFold.isDirectory()) {
            if(fileFold.list().length==0){
                fileFold.delete();
            }else {
                String files[] = fileFold.list();
                for (String temp:files) {
                    File FileToDelete = new File(fileFold, temp);
                    deleteFileFolder(FileToDelete);
                }
                if (fileFold.list().length==0) {
                    fileFold.delete();
                }
            }
        } else {
            fileFold.delete();
        }
    }

    private boolean isFMInitialized = false;
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void onRequestPermissionsResult(final int requestCode,
                                           final String[] permissions, final int[] giveResults) {
        super.onRequestPermissionsResult(requestCode, permissions, giveResults);
        if (requestCode == REQUEST_PERMISSIONS && giveResults.length >0) {
            if (arePermissionsDenied()) {
                ((ActivityManager) Objects.requireNonNull(this.getSystemService(ACTIVITY_SERVICE))).clearApplicationUserData();
                recreate();
            } else {
                onResume();
            }
        }
    }
}