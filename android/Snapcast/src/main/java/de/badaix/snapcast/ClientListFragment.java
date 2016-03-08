package de.badaix.snapcast;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import de.badaix.snapcast.control.json.Client;
import de.badaix.snapcast.control.json.ServerStatus;
import de.badaix.snapcast.control.json.Stream;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ClientItem.ClientInfoItemListener} interface
 * to handle interaction events.
 */
public class ClientListFragment extends Fragment {

    private static final String TAG = "ClientList";

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private Stream stream;

    private ClientItem.ClientInfoItemListener clientInfoItemListener;
    private ClientInfoAdapter clientInfoAdapter;
    private ServerStatus serverStatus = null;
    private boolean hideOffline = false;
    private TextView tvStreamState = null;

    public ClientListFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     * <p/>
     * //@param param1 Parameter 1.
     *
     * @return A new instance of fragment ClientListFragment.
     * // TODO: Rename and change types and number of parameters
     * public static ClientListFragment newInstance(String param1) {
     * ClientListFragment fragment = new ClientListFragment();
     * Bundle args = new Bundle();
     * args.putString(ARG_PARAM1, param1);
     * fragment.setArguments(args);
     * return fragment;
     * }
     */

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
//            mParam1 = getArguments().getString(ARG_PARAM1);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        Log.d(TAG, "onCreateView: " + this.toString());
        View view = inflater.inflate(R.layout.fragment_client_list, container, false);
        tvStreamState = (TextView) view.findViewById(R.id.tvStreamState);
        ListView lvClient = (ListView) view.findViewById(R.id.lvClient);
        clientInfoAdapter = new ClientInfoAdapter(getContext(), clientInfoItemListener);
        clientInfoAdapter.setHideOffline(hideOffline);
        clientInfoAdapter.updateServer(serverStatus);
        lvClient.setAdapter(clientInfoAdapter);
        updateGui();
        return view;
    }


    private void updateGui() {
        Log.d(TAG, "(tvStreamState == null): " + (tvStreamState == null) + " " + this.toString());
        if ((tvStreamState == null) || (stream == null))
            return;
        tvStreamState.setText(stream.getUri().getQuery().get("sampleformat") + " - " + stream.getStatus().toString());
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof ClientItem.ClientInfoItemListener) {
            clientInfoItemListener = (ClientItem.ClientInfoItemListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement ClientInfoItemListener");
        }
        updateGui();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        clientInfoItemListener = null;
    }

    public void updateServer(ServerStatus serverStatus) {
        this.serverStatus = serverStatus;
        if (clientInfoAdapter != null)
            clientInfoAdapter.updateServer(this.serverStatus);
    }

    public void setHideOffline(boolean hide) {
        this.hideOffline = hide;
        if (clientInfoAdapter != null)
            clientInfoAdapter.setHideOffline(hideOffline);
    }

    public String getName() {
        return stream.getName();
    }

    public void setStream(Stream stream) {
        Log.d(TAG, "setStream: " + stream.getName() + ", status: " + stream.getStatus());
        this.stream = stream;
        updateGui();
    }


    public class ClientInfoAdapter extends ArrayAdapter<Client> {
        private Context context;
        private ClientItem.ClientInfoItemListener listener;
        private boolean hideOffline = false;
        private ServerStatus serverStatus = new ServerStatus();

        public ClientInfoAdapter(Context context, ClientItem.ClientInfoItemListener listener) {
            super(context, 0);
            this.context = context;
            this.listener = listener;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Client client = getItem(position);
            final ClientItem clientItem;

            if (convertView != null) {
                clientItem = (ClientItem) convertView;
                clientItem.setClient(client);
            } else {
                clientItem = new ClientItem(context, client);
            }
            clientItem.setListener(listener);
            return clientItem;
        }

        public void updateServer(final ServerStatus serverStatus) {
            if (serverStatus != null) {
                ClientInfoAdapter.this.serverStatus = serverStatus;
                update();
            }
        }


        public void update() {

            ClientListFragment.this.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    clear();
                    for (Client client : ClientInfoAdapter.this.serverStatus.getClientInfos()) {
                        if ((client != null) && (!hideOffline || client.isConnected()) && !client.isDeleted() && client.getConfig().getStream().equals(ClientListFragment.this.stream.getId()))
                            add(client);
                    }
                    notifyDataSetChanged();
                }
            });
        }

        public boolean isHideOffline() {
            return hideOffline;
        }

        public void setHideOffline(boolean hideOffline) {
            if (this.hideOffline == hideOffline)
                return;
            this.hideOffline = hideOffline;
            update();
        }
    }


}
